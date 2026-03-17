package edu.automarket.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public record PageDTO<T>(List<T> content, long totalElements) {
    public <R> PageDTO<R> map(Function<T, R> mapper) {
        List<R> mappedContent = new ArrayList<>(content.size());
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < content.size(); i++) {
            T content = this.content.get(i);
            R contentDTO = mapper.apply(content);
            mappedContent.add(contentDTO);
        }
        return new PageDTO<>(mappedContent, totalElements);
    }
}
