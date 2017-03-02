# NGB documentation

NGB documentation is provided in a **markdown** format, that could be viewed directly from github or could be built into **html** representation

## Documentation index

The following sections are currently covered in a documentation
* Installation guide
    * [Overview](md/installation/overview.md)
    * [Using Docker](md/installation/docker.md)
    * [Using Binaries](md/installation/binaries.md)
    * [Using Standalone Jar](md/installation/standalone.md)
* Command Line Interface guide
    * [Overview](md/cli/introduction.md)
    * [CLI Installation](md/cli/installation.md)
    * [Running a command](md/cli/running-command.md)
    * [Accomplishing typical tasks](md/cli/typical-tasks.md)
    * [Command reference](md/cli/command-reference.md)
* User Interface Guide
    * [Overview](md/user-guide/overview.md)
    * [Working with datasets](md/user-guide/datasets.md)
    * [Working with tracks](md/user-guide/tracks.md)
    * [Reference track](md/user-guide/tracks-reference.md)
    * [Genes track](md/user-guide/tracks-genes.md)        
    * [VCF track](md/user-guide/tracks-vcf.md)
    * [WIG track](md/user-guide/tracks-wig.md)
    * [BED track](md/user-guide/tracks-bed.md)
    * [Aignments track](md/user-guide/tracks-bam.md)
    * [Working with Annotations](md/user-guide/annotations.md)

## Building documentation

[MkDocs](http://www.mkdocs.org) and `epam-material` theme (based on [mkdocs-material](https://github.com/squidfunk/mkdocs-material)) are used to build documentation into **html** representation

So make sure that both dependencies are installed according to the [installation guide](http://squidfunk.github.io/mkdocs-material/getting-started/#installing-mkdocs)

Once installed, obtain **markdown** sources from github
```
$ git clone https://github.com/epam/NGB.git
$ cd NGB/docs
```

Run build 
```
$ mkdocs build
```

This will create `site/` folder, containing built **html** documentation

To view documentation - navigate in `ngb/docs/site/` folder and launch `index.html` with a web-browser
