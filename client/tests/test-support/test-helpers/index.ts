import { ITrack } from "../types";
import fs from 'fs';
import { parse, join } from 'path';
import type { Page } from 'playwright';

const ENDPOINT_PREFIX = 'restapi';

function extractHARData(str) {
  const input = JSON.parse(str);
  const jsons = input.log.entries.filter((el) => {
      return el.response.content.mimeType === 'application/json' && el.request.url.includes(ENDPOINT_PREFIX);
  });
  
  const datasets = {};

  jsons.forEach((r) => {
      const key = r.request.url.split(ENDPOINT_PREFIX)[1];
      datasets[`${ENDPOINT_PREFIX}${key}`] = r.response.content.text;
  })
  
  return datasets;
}

export async function setupPage(page: Page) {
  page.setViewportSize({ width: 1500, height: 800 });
}

const hostname = `http://localhost:8080`;

export function buildUrl(
  referenceId: string,
  chromosome: string = "",
  start: string,
  end: string,
  tracks: ITrack[] = []
) {
  return `${hostname}/#/${[referenceId, chromosome, start, end]
    .filter((el) => el.length)
    .join("/")}?tracks=${encodeURIComponent(JSON.stringify(tracks))}`;
}


function readFile(file) {
  return fs.readFileSync(file, "utf8");
}

function addMixins(path) {
  const parts = parse(path);
  const dirname = fs.lstatSync(path).isDirectory() ? path : parts.dir;
  const files = fs.readdirSync(dirname).filter((el) => {
    return el.endsWith('.json') && el.startsWith(ENDPOINT_PREFIX);
  });
  const additions = {};
  files.forEach((name) => {
    const url = name.replace('.json', '').split('_').join('/');
    additions[url] = fs.readFileSync(join(dirname, name), 'utf8');
  });
  
  return additions;
}


export async function mockData(path, page) {
  let mocks = path.endsWith('.har') ? mockFromHAR(path) : mockFromJSON(path);
  const mockPaths = Object.keys(mocks);
  const routes = [];
  mockPaths.forEach((pathName) => {
    let route = page.route(`**/${pathName}`, (route) => {
        return route.fulfill({
          status: 200,
          body: mocks[pathName],
        })
      }
    );
    routes.push(route);
  });
  await Promise.all(routes);
}

function mockFromJSON(path) {
  let result = {};
  if (!fs.lstatSync(path).isDirectory()) {
    result =  JSON.parse(readFile(path));
  }
  let mixins = addMixins(path);
  return { ...mixins, ...result };
}

function mockFromHAR(path) {
  let result = {};
  if (!fs.lstatSync(path).isDirectory()) {
    result =   extractHARData(readFile(path));
  }
  let mixins = addMixins(path);
  return { ...mixins, ...result };
}
