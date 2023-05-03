package prs.project.controllers;

import lombok.Data;
import lombok.NonNull;
@Data
public class Handshake {
    @NonNull
    String  ip_addr;
    @NonNull
    Integer port;
    @NonNull
    Integer indeks;
}
