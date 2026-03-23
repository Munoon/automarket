package edu.automarket.common;

import org.springframework.http.HttpStatusCode;

public class ApiException extends RuntimeException {
    private final HttpStatusCode status;
    private final String type;
    private final String title;

    public ApiException(HttpStatusCode status, String type, String title) {
        super(title);
        this.status = status;
        this.type = type;
        this.title = title;
    }

    public ProblemDTO toProblem() {
        return new ProblemDTO(type, title, status.value());
    }

    public HttpStatusCode getStatus() {
        return status;
    }
}
