export function mapGooglePatentsData(data, analysisUrl) {
    const {
        articleData = {},
        contributor,
        description,
        snippet,
        pdfLink,
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
        pdfLink,
        analysisLink: pdfLink && analysisUrl ? `${analysisUrl}?url=${encodeURIComponent(pdfLink)}` : undefined
    };
}