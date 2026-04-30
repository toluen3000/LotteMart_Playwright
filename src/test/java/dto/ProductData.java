package dto;

public record ProductData(
        String testCaseId,
        String description,
        String url,
        String expectedName,
        String expectedPrice,
        String expectedSku,
        String searchTerm,
        String navigationPath,
        boolean expectedHasReview,
        String expectedReviewText
) {}