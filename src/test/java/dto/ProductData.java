package dto;

public record ProductData (
        String testCaseId,
        String url,
        String expectedName,
        String expectedPrice,
        boolean expectedHasReview,
        String expectedReviewText
) {
}