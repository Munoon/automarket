package edu.automarket.listing.dto;

public final class GetPublishedListingsRequestDTO {
    private Long publishedBefore;
    private int page;
    private int size = 20;

    public long getPublishedBefore() {
        if (publishedBefore == null) {
            publishedBefore = System.currentTimeMillis();
        }
        return publishedBefore;
    }

    public void setPublishedBefore(long publishedBefore) {
        this.publishedBefore = publishedBefore;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
