# Run CLI-SRV integration tests
# Script assumes that NGB Jar and CLI are already built and located at ../dist/ folder

TEST_DATA_URL="http://ngb.opensource.epam.com/distr/data/tests/"
TEST_DATA_PATH="$(pwd)/e2e/cli/test_data"
TEST_DATA_RELATIVE_PATH="e2e/cli/test_data"
DIST_PATH="$(pwd)/dist"
JAR_PATH="${DIST_PATH}/catgenome.jar"
CLI_PATH="${DIST_PATH}/ngb-cli.tar.gz"
CLI_BIN_PATH="${DIST_PATH}/ngb-cli/bin/ngb"
TEST_CASES_PATH="$(pwd)/e2e/cli/testcases.csv"

# Download test data
mkdir $TEST_DATA_PATH
wget -r -nH -nd -np -R index.html* -P $TEST_DATA_PATH $TEST_DATA_URL
cp "$TEST_DATA_PATH/CantonS.09-28.trim.dm606.realign_X_2L.bam" "$TEST_DATA_PATH/CantonS.09-28.trim.dm606.realign_X_2L_2.bam"

# Unpack CLI distribution
tar -zxf $CLI_PATH -C $DIST_PATH

# Run NGB server
nohup java -jar ${JAR_PATH} & 
printf 'Waiting for NGB to start'
until $(wget http://localhost:8080/catgenome); do
    sleep 3
done

# Run tests
./gradlew integrationCliTest -PcliPath=${CLI_BIN_PATH} -PresourcePath=${TEST_DATA_PATH} -PresourceRelativePath=${TEST_DATA_RELATIVE_PATH} -PtestCases=${TEST_CASES_PATH}

rm -r $TEST_DATA_PATH
rm catgenome
rm nohup.out
