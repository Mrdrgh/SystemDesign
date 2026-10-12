import os
import json
import time
import urllib.parse
import redis
import pymysql

REDIS_HOST = os.getenv("REDIS_HOST", "redis")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))
REDIS_QUEUE = os.getenv("REDIS_QUEUE", "pastebin_events")

MYSQL_HOST = os.getenv("MYSQL_HOST", "mysql")
MYSQL_PORT = int(os.getenv("MYSQL_PORT", "3306"))
MYSQL_USER = os.getenv("MYSQL_USER", "root")
MYSQL_PASSWORD = os.getenv("MYSQL_PASSWORD", "root")
MYSQL_DATABASE = os.getenv("MYSQL_DATABASE", "pastebin")


def get_redis_client():
    while True:
        try:
            r = redis.Redis(
                host=REDIS_HOST,
                port=REDIS_PORT,
                decode_responses=True,
                socket_timeout=None,
                health_check_interval=30,
            )
            r.ping()
            print(f"[WORKER] Successfully connected to Redis at {REDIS_HOST}:{REDIS_PORT}")
            return r
        except Exception as e:
            print(f"[WORKER] Waiting for Redis ({REDIS_HOST}:{REDIS_PORT})... error: {e}")
            time.sleep(2)


def get_db_connection():
    while True:
        try:
            conn = pymysql.connect(
                host=MYSQL_HOST,
                port=MYSQL_PORT,
                user=MYSQL_USER,
                password=MYSQL_PASSWORD,
                database=MYSQL_DATABASE,
                autocommit=True,
                cursorclass=pymysql.cursors.DictCursor,
            )
            print(f"[WORKER] Successfully connected to MySQL at {MYSQL_HOST}:{MYSQL_PORT}")
            return conn
        except Exception as e:
            print(f"[WORKER] Waiting for MySQL ({MYSQL_HOST}:{MYSQL_PORT})... error: {e}")
            time.sleep(2)


def extract_paste_url(event_payload):
    """
    Parses a MinIO / S3 notification JSON payload and extracts the paste URL.
    Handles both dict payloads and list-wrapped payloads: [ { ... } ]
    Object keys look like: pastes/<url>.txt (or pastebin/pastes/<url>.txt)
    """
    if isinstance(event_payload, list):
        for item in event_payload:
            url = extract_paste_url(item)
            if url:
                return url
        return None

    if not isinstance(event_payload, dict):
        return None

    raw_key = None

    # 1. MinIO 'Event' or AWS S3 'Records' list
    event_list = event_payload.get("Event") or event_payload.get("Records")
    if isinstance(event_list, list):
        for record in event_list:
            if isinstance(record, dict):
                s3_data = record.get("s3", {})
                object_data = s3_data.get("object", {})
                if "key" in object_data:
                    raw_key = object_data["key"]
                    break

    # 2. MinIO top-level 'Key' shortcut
    if not raw_key and "Key" in event_payload and event_payload["Key"]:
        raw_key = event_payload["Key"]

    # 3. Direct s3 object
    if not raw_key and "s3" in event_payload:
        raw_key = event_payload.get("s3", {}).get("object", {}).get("key")

    if not raw_key:
        return None

    # URL-decode in case of encoded characters (e.g., %2F)
    decoded_key = urllib.parse.unquote_plus(raw_key)

    if "pastes/" in decoded_key and decoded_key.endswith(".txt"):
        # Extract between 'pastes/' and '.txt'
        start = decoded_key.index("pastes/") + len("pastes/")
        end = decoded_key.rindex(".txt")
        return decoded_key[start:end]

    return None


def process_event(event_str, db_conn, redis_client):
    try:
        event = json.loads(event_str)
    except json.JSONDecodeError as e:
        print(f"[WORKER] [WARN] Failed to parse JSON event: {e}")
        return

    paste_url = extract_paste_url(event)
    if not paste_url:
        print(f"[WORKER] Event ignored (no matching paste key): {event}")
        return

    print(f"[WORKER] Detected upload event for paste url: '{paste_url}'")

    # Update MySQL
    try:
        with db_conn.cursor() as cursor:
            rows_updated = cursor.execute(
                """
                UPDATE paste 
                SET paste_status = 'AVAILABLE' 
                WHERE url = %s AND paste_status = 'UPLOAD_PENDING'
                """,
                (paste_url,),
            )
            if rows_updated > 0:
                print(f"[WORKER] [SUCCESS] MySQL updated: paste '{paste_url}' -> AVAILABLE")
            else:
                print(f"[WORKER] Paste '{paste_url}' was not in UPLOAD_PENDING status (already AVAILABLE or not found).")
    except (pymysql.OperationalError, pymysql.InterfaceError):
        # Reconnect on broken connection
        print("[WORKER] Database connection lost, reconnecting...")
        db_conn = get_db_connection()
        with db_conn.cursor() as cursor:
            cursor.execute(
                """
                UPDATE paste 
                SET paste_status = 'AVAILABLE' 
                WHERE url = %s AND paste_status = 'UPLOAD_PENDING'
                """,
                (paste_url,),
            )

    # Invalidate stale Redis cache so subsequent API calls fetch the new AVAILABLE status
    redis_client.delete(f"paste:{paste_url}")
    print(f"[WORKER] [SUCCESS] Cache invalidated for key: paste:{paste_url}")


def main():
    print("=" * 60)
    print("  PasteBin Upload Event Worker (Redis Queue Consumer)")
    print("=" * 60)

    redis_client = get_redis_client()
    db_conn = get_db_connection()

    print(f"[WORKER] Listening on Redis list '{REDIS_QUEUE}' using BLPOP...")

    while True:
        try:
            # BLPOP with a 5-second timeout keeps connection active and prevents socket timeouts
            result = redis_client.blpop(REDIS_QUEUE, timeout=5)
            if result:
                _, event_data = result
                process_event(event_data, db_conn, redis_client)
        except redis.exceptions.TimeoutError:
            # Normal socket idle timeout, keep waiting
            continue
        except redis.ConnectionError:
            print("[WORKER] Redis connection lost, reconnecting...")
            time.sleep(2)
            redis_client = get_redis_client()
        except Exception as e:
            print(f"[WORKER] [ERROR] Unexpected error in event loop: {e}")
            time.sleep(1)


if __name__ == "__main__":
    main()
