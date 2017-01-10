export const registerTrackLocalComputerMode = 'localComputer';
export const registerTrackNGBServerMode = 'ngbServer';
export const registerTrackS3BucketMode = 's3bucket';
export const registerTrackURLMode = 'http';

export default {
    registerModes: [
        {
            display: 'Local computer',
            disabled: true,
            value: registerTrackLocalComputerMode
        },
        {
            display: 'NGB Server',
            value: registerTrackNGBServerMode,
            isDefault: true
        },
        {
            display: 'S3',
            value: registerTrackS3BucketMode
        },
        {
            display: 'HTTP / FTP',
            value: registerTrackURLMode
        }
    ],
    names: {
        localComputerMode: registerTrackLocalComputerMode,
        ngbServerMode: registerTrackNGBServerMode,
        s3bucketMode: registerTrackS3BucketMode,
        urlMode: registerTrackURLMode
    }
};
