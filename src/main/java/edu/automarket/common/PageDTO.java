package edu.automarket.common;

import java.util.List;

public record PageDTO<T>(List<T> content, long totalElements) {
}
