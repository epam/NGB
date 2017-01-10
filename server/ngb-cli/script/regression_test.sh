#!/usr/bin/env bash

APT_HOME=$(pwd -W)


exit_with_error() {
    exit 1
}

register_reference() {
    REF_NAME=$1
#register reference
    if bash ngb reg_ref ${APT_HOME}/script/test_data/${REF_NAME} -n ${REF_NAME} -t | grep -q ${REF_NAME}; then
        echo "Reference ${REF_NAME} registered"
    else
        echo "Failed to register  ${REF_NAME}"
        exit_with_error
    fi

#check whether reference is available
    if bash ngb search ${REF_NAME} | grep -q ${REF_NAME}; then
        echo "Reference ${REF_NAME} found"
    else
        echo "Failed to find  ${REF_NAME}"
        exit_with_error
    fi
}

delete_reference() {
    FILE=$1
    NAME=$2
    echo "file ${FILE}"
    echo "name ${NAME}"
#delete file
    bash ngb del_ref ${FILE}
#check that we cannot find a deleted file
    if bash ngb search ${NAME} | grep -q 'No files found'; then
        echo "Reference ${NAME} successfully deleted"
    else
        echo "Failed to delete ${NAME}"
        exit_with_error
    fi
}

delete_file() {
    FILE=$1
    NAME=$2
#delete file
    bash ngb del_file ${FILE}
#check that we cannot find a deleted file
    if bash ngb search ${NAME} |  grep -q 'No files found'; then
        echo "File ${NAME} successfully deleted"
    else
        echo "Failed to delete ${NAME}"
        exit_with_error
    fi
}

register_file_with_index() {
    REF_NAME=$1
    FILE=$2
    INDEX=$3
#register file
    if bash ngb reg_file ${REF_NAME} ${APT_HOME}/script/test_data/${FILE}?${APT_HOME}/script/test_data/${INDEX} \
     -n ${FILE} -t | grep -q ${FILE}; then
        echo "File ${FILE} for reference ${REF_NAME} registered"
    else
        echo "Failed to register file ${FILE} for reference ${REF_NAME}"
        exit_with_error
    fi

#check whether file is available
    if bash ngb search ${FILE} | grep -q ${FILE}; then
        echo "File ${FILE} found"
    else
        echo "Failed to find ${FILE}"
        exit_with_error
    fi
}

check_search() {
    QUERY=$1
    EXPECTED_RESULT=$2
    if [[ $(bash ngb search ${QUERY} -j -l | wc -l) = ${EXPECTED_RESULT} ]]; then
        echo 'Substring search completed correctly'
    else
        echo 'Substring search failed'
        exit_with_error
    fi
}

check_search_empty() {
    QUERY=$1
    if bash ngb search ${QUERY} -l -j | grep -q 'No files found'; then
        echo 'Substring search completed correctly'
    else
        echo 'Substring search failed'
        exit_with_error
    fi
}

create_empty_dataset() {
    REF=$1
    NAME=$2
    if bash ngb reg_dataset ${REF} ${NAME} -j | grep -q ${NAME}; then
        echo "Dataset ${NAME} for reference ${REF} created"
    else
        echo "Failed to create a dataset ${NAME} for reference ${REF}"
        exit_with_error
    fi

    if bash ngb list_dataset -j | grep -q ${NAME}; then
        echo "Dataset ${NAME} found"
    else
        echo "Failed to find a dataset ${NAME}"
        exit_with_error
    fi
}

add_to_dataset() {
    DATASET=$1
    FILE=$2
    bash ngb add ${DATASET} ${FILE}
}

remove_dataset() {
      NAME=$1
      bash ngb del_dataset ${NAME}
      if bash ngb list_dataset -j | grep -q ${NAME}; then
         echo "Failed to delete a dataset ${NAME}"
         exit_with_error
      else
          echo "Dataset ${NAME} deleted"
      fi

}
echo "Starting regression script"
echo "Installing CLI"
gradle installDist -q
echo "Build successful"
cd ${APT_HOME}/build/install/ngb-cli/bin

#set test server URL
bash ngb set_srv http://localhost:8080//catgenome

REF1='toy.fa'
REF2='alt_toy.fa'
QUERY='toy'
BAM='toy.bam'
BAM_INDEX='toy.bam.bai'
DATASET1='toy_data_1'

#check empty search
check_search_empty ${QUERY}

register_reference ${REF1}
register_reference ${REF2}

REF1_ID=$(bash ngb search ${REF1} -j | grep -Po '(?<="Id":)\d+')
REF2_ID=$(bash ngb search ${REF2} -j | grep -Po '(?<="Id":)\d+')

register_file_with_index ${REF1} ${BAM} ${BAM_INDEX}

create_empty_dataset ${REF1} ${DATASET1}
add_to_dataset ${DATASET1} ${BAM}
remove_dataset ${DATASET1}

check_search ${QUERY} '3'

delete_file ${BAM} ${BAM}

check_search ${QUERY} '2'

#delete by name
delete_reference ${REF1} ${REF1}
#delete by id
delete_reference ${REF2} ${REF2}

#check empty search
check_search_empty ${QUERY}
