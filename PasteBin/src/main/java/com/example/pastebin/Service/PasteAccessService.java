package com.example.pastebin.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.pastebin.Entity.Paste;
import com.example.pastebin.Enum.AcessType;

@Service
public class PasteAccessService {
    private final Map<AcessType, PasteAccessStrategy> strategies;

    public PasteAccessService(List<PasteAccessStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(PasteAccessStrategy::getSupportedAcessType, Function.identity()));
    }

    public void checkAccess(Paste paste, AccessContext context) {
        PasteAccessStrategy strategy = strategies.get(paste.getAccessType());
        if (strategy == null) {
            throw new IllegalStateException("no strategy found for access type: " + paste.getAccessType());
        }

        strategy.validateAccess(paste, context);
    }
}
