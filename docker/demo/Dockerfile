# The core image plus the four demo references and the demo data set, ready for the `ngb reg_ref` /
# `ngb reg_file` / `ngb reg_dataset` commands in ../start-demo.sh. Published as
# lifescience/ngb:<version>-demo.
#
#   ./gradlew buildDockerDemo        # builds ngb:latest first, then this
#
ARG BASE_IMAGE=ngb:latest
FROM ${BASE_IMAGE}

# Where the data comes from. It was http://ngb.opensource.epam.com/distr/data, which stopped
# resolving; the same files are in the bucket the release build already publishes to, which is also
# where ../start-demo.sh has been reading them from.
ARG DATA_URL=https://ngb-oss-builds.s3.amazonaws.com/public/data

# What to bake in. The defaults are what the published image contains. Narrowing them is what makes
# this Dockerfile testable: the four references and the demo data are ~3.4 GB compressed and about
# 13 GB unpacked, so `--build-arg REFERENCES=dm6 --build-arg DEMO_DATA=false` is the build to run
# when the question is whether the image works rather than what is in it.
ARG REFERENCES="grch38 grch37 dm6 mm"
ARG DEMO_DATA=true

# Everything goes under ${NGS_DATA_DIR} (/ngs), because that is the value the base image's entrypoint
# writes into ngs.data.root.path, and since UrlValidatorService (Apr 2025) a local file outside that
# root cannot be registered at all. The references used to be baked into /reference and the demo data
# unpacked into /, both outside it - so the published demo image answered
# "Parameter path doesn't fall into 'ngs.data.root.path'" to its own registration commands.

# The FASTAs are gunzipped because NGB indexes an uncompressed reference; the GTF and BED files are
# registered as they are.
RUN set -eu; \
    for entry in \
            grch38/Homo_sapiens.GRCh38.gtf.gz \
            grch38/Homo_sapiens.GRCh38.fa.gz \
            grch38/Homo_sapiens.GRCh38.domains.bed \
            grch37/Homo_sapiens.GRCh37.gtf.gz \
            grch37/Homo_sapiens.GRCh37.fa.gz \
            dm6/dmel-all-r6.06.sorted.gtf.gz \
            dm6/dmel-all-chromosome-r6.06.fasta.gz \
            mm/Mus_musculus.GRCm38.sorted.gtf.gz \
            mm/Mus_musculus.GRCm38.fa.gz; do \
        ref="${entry%%/*}"; \
        file="${entry#*/}"; \
        case " ${REFERENCES} " in *" ${ref} "*) ;; *) continue ;; esac; \
        mkdir -p "${NGS_DATA_DIR}/reference/${ref}"; \
        wget --quiet -O "${NGS_DATA_DIR}/reference/${ref}/${file}" "${DATA_URL}/genome/${entry}"; \
        case "${file}" in \
            *.fa.gz | *.fasta.gz) gzip -d "${NGS_DATA_DIR}/reference/${ref}/${file}" ;; \
        esac; \
    done

RUN set -eu; \
    if [ "${DEMO_DATA}" = "true" ]; then \
        mkdir -p "${NGS_DATA_DIR}"; \
        wget --quiet -O /tmp/ngb_demo_data.tar.gz "${DATA_URL}/demo/ngb_demo_data.tar.gz"; \
        tar -zxf /tmp/ngb_demo_data.tar.gz -C "${NGS_DATA_DIR}"; \
        rm /tmp/ngb_demo_data.tar.gz; \
    fi

# What used to follow was a download of ngb-demo-index-cache.tar.gz, built against NGB 2.6.0. The
# on-disk index cache it warmed is gone: the EhCache-backed EhCacheBasedIndexCache was removed
# along with EhCache 2, and the Lucene indexes it complemented are 9.x
# and would have rejected the 2.6.0 files anyway. Nothing reads that directory now.

# ENTRYPOINT, EXPOSE, WORKDIR and CMD are inherited from the core image, which is where the JVM flags
# and the configuration live.
