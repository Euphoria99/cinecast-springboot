package com.stream.app.payload;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ErrorMessage {

    private String message;

    private boolean success=false;

}
