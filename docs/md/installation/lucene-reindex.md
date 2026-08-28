# Rebuilding the Lucene indexes

This release changes the search-index format. **A fresh installation needs nothing from this
page** — start NGB and it builds its indexes as data is registered. Read on only if you are
upgrading an NGB instance that already has data.

| | Previous releases | This release |
|---|---|---|
| Lucene | 6.6.0 | 9.12.3 |

NGB stores everything it searches — variations, genes, taxonomy, homologues, drug/disease
associations, BAM coverage, pathways — in Lucene indexes on disk, next to the registered files.
**Lucene 9 cannot read an index written by Lucene 6, and cannot upgrade one in place.** Every
index NGB wrote before this release has to be rebuilt once.

Nothing else is affected. The database, the registered files and their tabix/bai indexes are all
untouched, and no index is deleted for you.

## What you have to do, in short

1. Back up, and stop NGB.
2. Start NGB. It will refuse to start and list the global index directories it cannot read.
3. Delete or move the directories it listed.
4. Start NGB again — it comes up with those indexes empty.
5. Run the rebuild call for each one, from the [table below](#rebuilding-a-global-index).
6. Reindex the registered VCF, GFF/GTF and BED files, either up front or as you hit them —
   see [per-file feature indexes](#per-file-feature-indexes).

Steps 2–4 are one shutdown, not two: if you already know from the table which directories you
have, delete them before the first start and NGB will come straight up.

## Two kinds of index

They fail differently, and are rebuilt differently.

* **Global indexes** — one per data source, configured by a `*.index.directory` property. NGB
  checks all of them **at startup** and refuses to start if any is unreadable, because a server
  whose taxonomy or targets index cannot be opened is not going to serve anything useful from it.
* **Per-file feature indexes** — one per registered VCF, GFF/GTF or BED file, inside
  `files.base.directory.path`. There can be thousands of these, so they are **not** checked at
  startup; each one is reported the first time that file is searched, and rebuilt with a single
  call that needs no manual deletion.

## What the startup refusal looks like

```
***************************
APPLICATION FAILED TO START
***************************

Description:

NGB will not start: 14 Lucene index director(y/ies) cannot be read by Lucene 9.12.3, which is what this release uses.

They were written by NGB 2.8 or earlier, whose Lucene was 6. Lucene 9.12.3 can neither read that format nor upgrade it in place, so each of these has to be rebuilt once. Nothing has been deleted and no other data - not the database, not the registered files - is affected.

  /opt/ngb/contents/taxonomy
      to rebuild: PUT /restapi/taxonomy/upload?taxonomyFilePath=<NCBI names.dmp>

  /opt/ngb/contents/homologene
      to rebuild: PUT /restapi/homologene/import?databasePath=<homologene.xml>

  /opt/ngb/contents/pathway
      to rebuild: PUT /restapi/pathway/index - rebuilds every registered pathway from the database and the pathway files, no arguments needed

  /opt/ngb/contents/coverage
      to rebuild: PUT /restapi/bam/coverage/index - recomputes every registered coverage track from its BAM file; long-running

  /opt/ngb/contents/targets/opentargets.disease
      to rebuild: PUT /restapi/target/import/opentargets?path=<Open Targets download>

  ... one entry per unreadable directory ...

To continue: stop NGB, delete or move the director(y/ies) listed above, and start NGB again. It will come up with those indexes empty. Then run the rebuild call shown for each one.

Per-file feature indexes (VCF, GFF/GTF, BED) are deliberately not checked here - there can be thousands of them. They are reported when the file is first read, and rebuilding one is a single call that needs no manual deletion.

Full procedure: docs/md/installation/lucene-reindex.md
```

The list is the authoritative one for *your* installation: it names the directories that actually
exist and are actually stale, resolved from your configuration. A directory that is absent, empty,
or already in the new format is not listed.

On a successful start you get one line instead, on standard output:

```
INFO  Lucene index version check passed: 14 index director(y/ies) under ./contents/taxonomy, ./contents/homologene, ./contents/pathway, ./contents/coverage, ./contents/targets, ./contents/ncbi are readable by Lucene 9.12.3.
```

and, between deleting the stale directories and rebuilding them:

```
INFO  Lucene index version check: the configured index director(y/ies) ./contents/targets, ./contents/ncbi hold no Lucene index yet, so there is nothing to verify. Expected on a new installation, and straight after deleting an index to rebuild it.
```

## Rebuilding a global index

Every directory below sits under the property in the second column; the defaults shown are the
ones the standalone JAR ships with. `<base>` is `files.base.directory.path`.

| Directory | Configured by | Default | Rebuild with |
|---|---|---|---|
| `taxonomy` | `taxonomy.index.directory` | `<base>/taxonomy` | `PUT /restapi/taxonomy/upload?taxonomyFilePath=<names.dmp>` |
| `homologene` | `homologene.index.directory` | `<base>/homologene` | `PUT /restapi/homologene/import?databasePath=<homologene.xml>` |
| `pathway` | `pathway.index.directory` | `<base>/pathway` | `PUT /restapi/pathway/index` |
| `coverage` | `bam.coverage.index.directory` | `<base>/coverage` | `PUT /restapi/bam/coverage/index` |
| `targets/opentargets.*` (4) | `targets.index.directory` | `<base>/targets/` | `PUT /restapi/target/import/opentargets?path=<dir>` |
| `targets/dgidb.*` (1) | `targets.index.directory` | `<base>/targets/` | `PUT /restapi/target/import/dgidb?path=<file>` |
| `targets/pharmgkb.*` (4) | `targets.index.directory` | `<base>/targets/` | `PUT /restapi/target/import/pharmGKB?genePath=&drugPath=&drugAssociationPath=&diseaseAssociationPath=` |
| `targets/ttd.*` (2) | `targets.index.directory` | `<base>/targets/` | `PUT /restapi/target/import/ttd?drugsPath=&targetsPath=&diseasesPath=` |
| `targets/genes`, `targets/gene.fields` | `targets.index.directory` | `<base>/targets/` | `POST /restapi/target/genes/import/{targetId}` — **see [the exception](#targetsgenes-uploaded-gene-lists) below** |
| `ncbi/gene.ids` | `ncbi.index.directory` | `<base>/ncbi/` | `PUT /restapi/externaldb/ncbi/genes/import?path=<gene2ensembl>` |
| `ncbi/gene.info` | `ncbi.index.directory` | `<base>/ncbi/` | `PUT /restapi/externaldb/ncbi/genes/info/import?path=<gene_info>` |

Notes:

* **The import calls are the same ones you originally used.** They need the same source files:
  the NCBI taxonomy dump, the HomoloGene XML, the Open Targets / DGIdb / PharmGKB / TTD
  downloads. If you no longer have them, download them again — and take the opportunity to take
  a current release rather than reproducing an old one.
* **`pathway` and `coverage` need no source files.** They are rebuilt from the database and from
  the registered pathway and BAM files, which is why those two calls take no arguments. Both
  endpoints are new in this release, added so that these two indexes are not a dead end after an
  upgrade.
* **`PUT /restapi/bam/coverage/index` re-reads every registered BAM.** On a large installation
  this runs for a long time. `?coverageId=<id>` restricts it to one track, which is useful for
  repairing a single track later, but *not* for this upgrade: it cannot open the stale index to
  delete one track's documents from it. For the upgrade, delete the directory and call it with no
  arguments.
* Only the directories NGB reported need deleting. `<base>/targets/` also holds per-target
  subdirectories; leave them alone.
* All of these are administrator operations. With authentication enabled, send
  `Authorization: Bearer <JWT>` (a token for a user with `ROLE_ADMIN`, or the specific manager
  role — `ROLE_PATHWAY_MANAGER` for pathways, `ROLE_BAM_MANAGER` for coverage).

Example, with the server on `localhost:8080` and authentication disabled:

```bash
curl -X PUT 'http://localhost:8080/catgenome/restapi/pathway/index'
# {"payload":3,"status":"OK"}   <- three pathways reindexed

curl -X PUT 'http://localhost:8080/catgenome/restapi/bam/coverage/index'
# {"payload":2,"status":"OK"}   <- two coverage tracks recomputed
```

### `targets/genes`: uploaded gene lists

One index cannot be rebuilt from anything NGB kept. When you attach a gene list to a target by
uploading a spreadsheet (`POST /restapi/target/genes/import/{targetId}`, or **Import genes** in
the UI), NGB parses the file straight into `targets/genes` and `targets/gene.fields` and does not
store the file. There is nothing on disk to rebuild those two indexes from.

So: **before upgrading, make sure you still have the xlsx/csv/tsv files you uploaded for every
target.** After the upgrade, re-upload each of them:

```bash
curl -X POST 'http://localhost:8080/catgenome/restapi/target/genes/import/42?path=/ngs/targets/my-genes.xlsx'
```

The targets themselves — their names, species, identifiers and the associations imported from Open
Targets and the other sources — are in the database and are not affected. It is only the uploaded
gene rows that are index-only.

If those files are genuinely gone, the targets will come back with an empty gene list and have to
be re-populated by hand before identification reports will run for them.

## Per-file feature indexes

One per registered VCF, GFF/GTF or BED file, at
`<base>/42/{VCF,genes,bed}/<file id>/index.luc`. They are what backs variation search, gene
search, and the filters in those tracks.

These are **not** checked at startup. The first search that touches a stale one returns an error
naming the file and the call that fixes it, and the rest of NGB keeps working:

```json
{
  "status": "ERROR",
  "message": "The Lucene index in '/opt/ngb/contents/42/VCF/1/index.luc' was written by an older release of NGB and cannot be read by Lucene 9.12.3, which is what this release uses. There is no in-place upgrade - the index has to be rebuilt once, and nothing outside that directory is affected. To rebuild it: delete nothing by hand, just call GET /restapi/vcf/1/index - it removes the stale index directory before rebuilding (NGB CLI: ngb index_file 1). Full procedure: docs/md/installation/lucene-reindex.md"
}
```

| File type | Rebuild with | CLI |
|---|---|---|
| VCF | `GET /restapi/vcf/{vcfFileId}/index` | `ngb index_file <id>` |
| GFF/GTF, featureCounts | `GET /restapi/gene/{geneFileId}/index?full=true` | `ngb index_file <id>` |
| BED | `GET /restapi/bed/{bedFileId}/index` | `ngb index_file <id>` |

Unlike a global index, **you do not delete anything first** — these endpoints drop the old index
directory before rebuilding it.

`full=true` on a gene file reindexes from the original GFF/GTF. Without it, NGB indexes the
preprocessed large-scale and transcript files it derived at registration time, which is faster;
either produces a working index.

SEG and WIG files have no feature index and need nothing.

### Doing them all at once

You do not have to. An un-reindexed file behaves exactly as it did before you searched it —
the track still displays, only search and filtering fail, with the message above. Reindex as
you go if that suits you better.

To do them up front, list the file ids from the dataset tree and reindex each. With
[jq](https://jqlang.github.io/jq/):

```bash
NGB=http://localhost:8080/catgenome/restapi

curl -s "$NGB/project/tree" \
  | jq -r '.. | objects | select(.format? == "VCF" or .format? == "GENE"
                                 or .format? == "FEATURE_COUNTS" or .format? == "BED")
                | "\(.format) \(.id)"' \
  | sort -u \
  | while read -r format id; do
      case "$format" in
        VCF)                 path="vcf/$id/index" ;;
        GENE|FEATURE_COUNTS) path="gene/$id/index?full=true" ;;
        BED)                 path="bed/$id/index" ;;
      esac
      echo "reindexing $format $id"
      curl -s -X GET "$NGB/$path" > /dev/null
    done
```

Reindexing is CPU- and IO-bound and holds no lock on anything else; run it while the server is
serving, but expect it to be slow on large VCFs.

**_Note_**: `project/tree` lists files inside datasets. **Reference-linked gene files and BED annotations** (e.g. `*_Genes`, `*_Domains`) are attached to the reference, not a dataset - add them too:

```bash
curl -s "$NGB/reference/loadAll" \
  | jq -r '.payload[] | (.geneFile.id // empty | "GENE \(.)"),
                        (.annotationFiles[]?.id | "BED \(.)")'
```

Then reindex those found with the same GENE/BED endpoints:

| File type | Rebuild with | CLI |
|---|---|---|
| GENE | `curl -s -X GET $NGB/gene/<geneFileId>/index?full=true > /dev/null` | `ngb index_file <id>` |
| BED | `curl -s -X GET $NGB/bed/<bedFileId>/index > /dev/null` | `ngb index_file <id>` |

Verify a rebuild landed on the new codec:

```bash
find <base>/contents -name 'segments*' -exec sh -c 'grep -ao "Lucene[0-9]*" "$1"' _ {} \;
# expect Lucene9* (not Lucene6*)
```

## Verifying

Once the global indexes are back and at least one VCF and one GFF/GTF have been reindexed:

| Check | How |
|---|---|
| variation search | the **Variants** panel on that dataset, or `POST /restapi/filter` |
| gene search | `GET /restapi/gene/search?geneId=<gene name>` |
| taxonomy | `GET /restapi/taxonomies/{term}` |
| homologues | `GET /restapi/homologene/search?geneIds=<gene name>` |
| target identification | `POST /restapi/target/identification` for an existing target |
| BAM coverage | open the coverage track, or `POST /restapi/bam/coverage/search` |
| pathways | the **Pathways** panel, or `POST /restapi/pathways` |

The startup log line quoted [above](#what-the-startup-refusal-looks-like) is the quickest overall
check: it counts the index directories it managed to open.

## If something goes wrong

* **NGB starts, but a search returns "was written by an older release of NGB".** A per-file
  feature index that has not been reindexed yet. The message names the call.
* **NGB refuses to start again after you deleted the directories.** Check you deleted the ones it
  listed, and that the parent directory is writable by the NGB user. NGB recreates them itself.
* **`Could not check the Lucene index version in '...'`, and NGB starts anyway.** The check could
  not read that directory at all — a permission problem or a broken mount, not the stale-index
  case. NGB does not refuse to start over it, but the manager that needs the index will fail, so
  fix the directory.
* **An import call fails with "was written by an older release of NGB".** You are re-running an
  import over a directory you have not deleted, and that import appends rather than rebuilds.
  Delete the directory and run it again.

Nothing here is reversible in the sense of getting the Lucene 6 index back — but nothing needs to
be: the indexes are derived data. Your backup covers the case where something else goes wrong.
