export function mapGooglePatentsData(data) {
    const {
        articleData = {},
        contributor,
        description,
        snippet,
        ...rest
    } = data;
    const {
        assignee,
        filingDate,
        inventor,
        publicationDate,
        publicationNumber,
    } = articleData;
    return {
        ...rest,
        snippet,
        description: description || snippet,
        assignee,
        filingDate,
        inventor: inventor || contributor,
        publicationDate,
        publicationNumber,
    };
}