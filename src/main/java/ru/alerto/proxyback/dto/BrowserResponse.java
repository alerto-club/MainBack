package ru.alerto.proxyback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BrowserResponse {
    private Long tgId;
    private List<DirectoryDto> dirs;
    private String correctDirectory;
}