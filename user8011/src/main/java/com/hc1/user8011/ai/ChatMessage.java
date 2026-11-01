package com.hc1.user8011.ai;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private String role;    // "user" 或 "assistant"
    private String content;



}