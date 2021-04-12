import type { ITrack } from "../types";
const tracks = {};
export default function tracksFor(key: keyof typeof tracks): ITrack[] {
    return tracks[key];
}
  