#!/usr/bin/env bash
# Does the lombok version this build resolves work under javac 21?
#
# This is migration checkpoint 1's second half. Gradle 3.3 refuses to start on JDK 21 at
# all, which hides the next wall behind it: even with a modern Gradle, lombok 1.16.16
# cannot run as an annotation processor on JDK 21 - it reaches into javac internals that
# JDK 16+ no longer expose. Since ~1200 files in server/catgenome use lombok annotations,
# nothing compiles until lombok is raised (>= 1.18.30 for JDK 21).
#
# Run via `make probe-java21`. Exits 1 when JDK 21 fails, which is the expected result
# today - the Makefile ignores the status.
set -uo pipefail

LOMBOK=$(find "${GRADLE_USER_HOME:-$HOME/.gradle}" -name 'lombok-*.jar' 2>/dev/null | head -1)
if [ -z "$LOMBOK" ]; then
    echo "lombok jar not in the gradle cache - run a build first (make jar-fast)"
    exit 0
fi
echo "lombok: $(basename "$LOMBOK")"

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
cat > "$work/Probe.java" <<'EOF'
import lombok.Getter;

@Getter
public class Probe {
    private final String value = "x";
}
EOF

# No -proc flag: annotation processing on a classpath processor is the default on both
# JDKs, and -proc:full does not exist before JDK 9.
probe() {  # $1 = with-java8 | with-java21
    local out rc
    out=$("$1" javac -cp "$LOMBOK" -d "$work" "$work/Probe.java" 2>&1)
    rc=$?
    if [ $rc -eq 0 ]; then
        echo "  compiled OK"
    else
        echo "$out" | grep -Ev '^(Note:|  |$)' | head -6 | sed 's/^/  /'
    fi
    return $rc
}

echo "-- javac 8 --"
probe with-java8
echo "-- javac 21 --"
probe with-java21
