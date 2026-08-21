#!/usr/bin/env python3
"""One-shot mechanical rewrite of the Lucene 6 API onto Lucene 9, migration Phase 6.

Kept in the repo because it documents exactly which transformations were applied to which
call sites, which is otherwise only visible as a 100-file diff. It is idempotent: running it
again on an already-swept tree changes nothing.

  new SimpleFSDirectory(Paths.get(x))  ->  LuceneIndexUtils.openDirectory(x)
  SimpleFSDirectory (as a type)        ->  FSDirectory
  DirectoryReader.open(d)              ->  LuceneIndexUtils.openReader(d)
  new IndexWriter(d, cfg)              ->  LuceneIndexUtils.openWriter(d, cfg)
                                       ->  ...openWriterForRebuild(...) at the deleteAll() sites

SimpleFSDirectory and the reader/writer constructors are routed through LuceneIndexUtils so the
D8 guard has a single choke point; see that class for why.
"""
import re
import sys
from pathlib import Path

SRC = Path(sys.argv[1] if len(sys.argv) > 1 else
           "server/catgenome/src/main/java/com/epam/catgenome")

UTILS_IMPORT = "import com.epam.catgenome.util.LuceneIndexUtils;"
FSDIR_IMPORT = "import org.apache.lucene.store.FSDirectory;"
SIMPLE_IMPORT = "import org.apache.lucene.store.SimpleFSDirectory;"


def add_import(text, statement, after_prefixes):
    if statement in text:
        return text
    lines = text.split("\n")
    anchor = None
    for i, line in enumerate(lines):
        if any(line.startswith(p) for p in after_prefixes):
            anchor = i
    if anchor is None:
        for i, line in enumerate(lines):
            if line.startswith("import "):
                anchor = i
                break
    if anchor is None:
        return text
    lines.insert(anchor + 1, statement)
    return "\n".join(lines)


def upgrade_rebuild_writers(text):
    """Only a writer whose first statement is deleteAll() may discard an unreadable index.

    That is the whole justification for openWriterForRebuild - such a call site has already
    declared the directory contents disposable. Decided per call site, not per file:
    AbstractIndexManager has both an importData() that qualifies and a delete(Query) that must
    not. Matched by walking back from each deleteAll() to the writer that opened it.
    """
    lines = text.split("\n")
    for i, line in enumerate(lines):
        if "writer.deleteAll();" not in line:
            continue
        for j in range(i - 1, max(i - 8, -1), -1):
            if "LuceneIndexUtils.openWriter(" in lines[j]:
                lines[j] = lines[j].replace("LuceneIndexUtils.openWriter(",
                                            "LuceneIndexUtils.openWriterForRebuild(")
                break
    return "\n".join(lines)


# The guard itself is the one place that legitimately calls the raw Lucene constructors.
SKIP = {"util/LuceneIndexUtils.java"}


def sweep(path):
    rel = str(path.relative_to(SRC))
    if rel in SKIP:
        return False
    original = text = path.read_text()

    # Directories. Paths.get(...) collapses into the helper, which takes a String or a Path.
    text = re.sub(r"new SimpleFSDirectory\(Paths\.get\(([^()]*(?:\([^()]*\)[^()]*)*)\)\)",
                  r"LuceneIndexUtils.openDirectory(\1)", text)
    text = re.sub(r"new SimpleFSDirectory\(", "LuceneIndexUtils.openDirectory(", text)
    # ...then the remaining occurrences are type references.
    text = re.sub(r"\bSimpleFSDirectory\b", "FSDirectory", text)

    # Readers and writers.
    text = re.sub(r"\bDirectoryReader\.open\(", "LuceneIndexUtils.openReader(", text)
    text = re.sub(r"new IndexWriter\(", "LuceneIndexUtils.openWriter(", text)
    text = upgrade_rebuild_writers(text)

    if text == original:
        return False

    text = text.replace(SIMPLE_IMPORT + "\n", "")
    if re.search(r"\bFSDirectory\b", text):
        text = add_import(text, FSDIR_IMPORT, ("import org.apache.lucene.store.",
                                               "import org.apache.lucene."))
    if "LuceneIndexUtils." in text and "package com.epam.catgenome.util;" not in text:
        text = add_import(text, UTILS_IMPORT, ("import com.epam.catgenome.",))
    # DirectoryReader was often imported only for its static open().
    if not re.search(r"\bDirectoryReader\b", text.replace(
            "import org.apache.lucene.index.DirectoryReader;", "")):
        text = text.replace("import org.apache.lucene.index.DirectoryReader;\n", "")

    path.write_text(text)
    return True


changed = [p for p in sorted(SRC.rglob("*.java")) if sweep(p)]
for p in changed:
    print(p.relative_to(SRC))
print(f"{len(changed)} files rewritten")
