#!/bin/bash
#
# This is a NGB build automation script. It performs building of client and server components together.
# Project directory should look the following way:
# /project_root
# |
# |-client - UI project's directory
#    |-client
#    |-package.json
#    ...
# |-server - server project's directory
#    |-catgenome
#    |-ngb-cli
#    |-build.gradle
#    ...
# |-build.sh - this script
#
# Script's result will be located in the dist directory

ALL=false
WAR=false
CLI=false
DOC=false
DOCKER=false

# extract options and their arguments into variables.
while [ $# -gt 0 ]; do
    case "$1" in
        -w|--war) WAR=true ; shift ;;
        -c|--cli) CLI=true ; shift ;;
        -d|--docs) DOC=true ; shift ;;
		-i|--docker) DOCKER=true ; shift ;;    
		-a|--all) ALL=true ; shift ;;
        --) shift ; break ;;
        *) echo "Unrecognised option" ; exit 1 ;;
    esac
done

if [ "$WAR" = false ] && [ "$DOC" = false ] && [ "$CLI" = false ] && [ "$DOCKER" = false ]; then
    ALL=true
fi    

BUILD_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# client is built for full build, war and docker
if [ "$ALL" = true ] || [ "$WAR" = true ] || [ "$DOCKER" = true ]; then
    echo ""
    echo '################################################'
    echo '                Building NGB UI'
    echo '################################################'
	
	(cd $BUILD_DIR/server/catgenome/src/main/webapp/ && ls | grep -v 'WEB-INF' | grep -v 'swagger-ui' | xargs rm -rf)
	
    # Here we build NGB UI using webpack and npm and copy it to server's webapp directory
	# Handle Posix path conversion for Windows shells
	# double '//' will be converted into single '/', for Unix it isn't required
    CATGENOME="/catgenome"
    case "`uname`" in
        MINGW* )
            CATGENOME="//catgenome"
            ;;
    esac
    (cd $BUILD_DIR/client && npm prune && npm install && npm run build:prod -- --publicPath $CATGENOME)
    cp $BUILD_DIR/client/dist/* $BUILD_DIR/server/catgenome/src/main/webapp/
    sleep 5
fi


# Here we build NGB server with UI using gradle
if [ "$ALL" = true ] || [ "$WAR" = true ] || [ "$DOCKER" = true ] || [ "$CLI" = true ]; then
    cd $BUILD_DIR/server 
    chmod +x gradlew
fi

# build server
if [ "$ALL" = true ] || [ "$WAR" = true ] || [ "$DOCKER" = true ]; then
    
    echo ""
    echo '################################################'
    echo '              Building NGB Server'
    echo '################################################'    
    #For Jenkins builds
    BUILD=$BUILD_NUMBER
    if [[ $BUILD =  *[!\ ]* ]]; then
        ./gradlew catgenome:clean catgenome:build catgenome:war -Pprofile=release -PbuildNumber=$BUILD
    else
        ./gradlew catgenome:clean catgenome:build catgenome:war -Pprofile=release
    fi    

    if [ ! -d "$BUILD_DIR/dist" ]; then
        mkdir $BUILD_DIR/dist
    else
        if [ -f $BUILD_DIR/dist/catgenome.war ];then        
            rm $BUILD_DIR/dist/catgenome.war
        fi
    fi
    cp $BUILD_DIR/server/catgenome/build/libs/catgenome.war $BUILD_DIR/dist/
fi

#build CLI
if [ "$ALL" = true ] || [ "$CLI" = true ] || [ "$DOCKER" = true ]; then
    echo ""
    echo '################################################'
    echo '              Building NGB CLI'
    echo '################################################'    
    ./gradlew ngb-cli:clean ngb-cli:build

    if [ ! -d "$BUILD_DIR/dist" ]; then
        mkdir $BUILD_DIR/dist
    else
        if [ -f $BUILD_DIR/dist/ngb-cli.tar ];then
            rm $BUILD_DIR/dist/ngb-cli.tar
        fi
    fi
    cp $BUILD_DIR/server/ngb-cli/build/distributions/ngb-cli.tar $BUILD_DIR/dist/
fi

#build docs
if [ "$ALL" = true ] || [ "$DOC" = true ] ; then
    echo ""
    echo '################################################'
    echo '              Building DOCUMENTATION'
    echo '################################################'        
    
    BUILD=$BUILD_NUMBER
    if [[ $BUILD =  *[!\ ]* ]]; then
        VERSION=2.1.$BUILD
    else
        VERSION=2.1
    fi    
    if [ -f "$BUILD_DIR/docs/site" ]; then
        rm -rf $BUILD_DIR/docs/site
    fi   

    if [ -f "$BUILD_DIR/docs/ngb_docs_*" ]; then
		rm $BUILD_DIR/docs/ngb_docs_*
    fi

    if [ ! -d "$BUILD_DIR/dist" ]; then
        mkdir $BUILD_DIR/dist
    fi

    cd $BUILD_DIR/docs && mkdocs build && tar -zcvf ngb_docs_${VERSION}.tar.gz site && cp ngb_docs_${VERSION}.tar.gz $BUILD_DIR/dist/    
	rm -rf $BUILD_DIR/docs/site
	rm $BUILD_DIR/docs/ngb_docs_*
fi

#build docker image
if [ "$ALL" = true ] || [ "$DOCKER" = true ]; then
    echo ""
    echo '################################################'
    echo '              Building DOCKER'
    echo '################################################'    
    if [ -f $BUILD_DIR/docker/core/ngb-cli.tar ]; then
        rm $BUILD_DIR/docker/core/ngb-cli.tar
    fi    

    if [ -f $BUILD_DIR/docker/core/catgenome.war ]; then
        rm $BUILD_DIR/docker/core/catgenome.war
    fi

    cp $BUILD_DIR/dist/ngb-cli.tar $BUILD_DIR/docker/core
    cp $BUILD_DIR/dist/catgenome.war $BUILD_DIR/docker/core

    cd $BUILD_DIR/docker/core
    docker build -t ngb:latest .
fi
