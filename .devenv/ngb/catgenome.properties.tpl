# Rendered by entrypoint.sh into /opt/ngb/config/catgenome.properties.
# Overrides the defaults baked into the jar (profiles/jar/catgenome.properties):
# this file is loaded last, so its keys win.

server.port=${SERVER_PORT}

# data locations inside the container
files.base.directory.path=/opt/ngb/contents
files.download.directory.path=/opt/ngb/download
config.path=/opt/ngb/config

# /ngs is the bind-mounted .devenv/data/ngs directory on the host
ngs.data.root.path=/ngs
file.browsing.allowed=true
url.browsing.allowed=true
url.browsing.allowed.hosts=*

# database
database.driver.class=${DB_DRIVER}
database.jdbc.url=${DB_URL}
database.username=${DB_USER}
database.password=${DB_PASSWORD}
database.max.pool.size=10
database.initial.pool.size=5

base.external.url=${NGB_BASE_URL}

# feature indexes and caches under the contents volume
taxonomy.index.directory=/opt/ngb/contents/taxonomy
homologene.index.directory=/opt/ngb/contents/homologene
pathway.index.directory=/opt/ngb/contents/pathway
biopax.directory=/opt/ngb/contents/biopax
bam.coverage.index.directory=/opt/ngb/contents/coverage
targets.index.directory=/opt/ngb/contents/targets/
ncbi.index.directory=/opt/ngb/contents/ncbi/
targets.alignment.directory=/opt/ngb/contents/targets/
server.index.cache.enabled=true

# smaller than the 512MB production default - keeps indexing memory sane in a container
search.indexer.buffer.size=128

# ProteinPatentsScheduledService is unconditionally @Scheduled and needs a patents
# database this env has no reason to ship, so at the 60s default it stack-traces every
# minute and buries everything else. 30 days keeps one startup error and no more.
targets.sequence.patents.search.rate=2592000000
