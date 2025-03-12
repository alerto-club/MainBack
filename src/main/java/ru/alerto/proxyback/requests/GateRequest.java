package ru.alerto.proxyback.requests;

import lombok.*;

import java.util.List;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class GateRequest {
    private Long tgId;
    private List<Directory> dirs;
    private String correctDirectory;
}
