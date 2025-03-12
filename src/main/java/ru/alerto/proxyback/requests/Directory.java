package ru.alerto.proxyback.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Directory {
    @JsonProperty("directoryTitle")
    private String directoryTitle;

    @JsonProperty("directoryId")
    private Long directoryId;
}