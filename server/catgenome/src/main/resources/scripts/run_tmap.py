import pandas as pd
import tmap
from faerun import Faerun
from mhfp.encoder import MHFPEncoder
from colorhash import ColorHash
from matplotlib.colors import ListedColormap
import os.path
import argparse


def get_color_map(df, col_name, map_name):
    genes = list(df[col_name].unique())

    colors = {}
    for g in genes:
        colors[g] = ColorHash(g).hex

    return ListedColormap(list(colors.values()), name=map_name)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--drugs', required=True)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()

    if not os.path.exists(args.output):
        os.mkdir(args.output)

    df = pd.read_csv(args.drugs)
    df['Gene Name'] = df['Gene Name'].astype('category')
    df['DRUG_NAME'] = df['DRUG_NAME'].astype('category')

    perm = 512
    enc = MHFPEncoder(perm)
    fingerprints = [tmap.VectorUint(enc.encode(s)) for s in df["SMILES"]]
    lf = tmap.LSHForest(perm)
    lf.batch_add(fingerprints)
    lf.index()
    x, y, s, t, _ = tmap.layout_from_lsh_forest(lf)

    gene_labels = list(enumerate(df["Gene Name"].value_counts().index.to_list()))
    gene_cmap = get_color_map(df, 'Gene Name', 'gene_cmap')

    faerun = Faerun(view="front", coords=False)
    faerun.add_scatter(
        "TMAP_Plot",
        {"x": x,
         "y": y,
         "c": [list(df["Gene Name"].cat.codes)],
         "labels": df["SMILES"].astype(str) + '__' + df["Gene Name"].astype(str) + '__' + df["DRUG_NAME"].astype(str)
         },
        point_scale=5,
        colormap=[gene_cmap],
        has_legend=True,
        categorical=[True],
        series_title=['Gene Name'],
        legend_labels=[gene_labels],
        shader='smoothCircle'
    )

    faerun.add_tree("TMAP_Plot_tree", {"from": s, "to": t}, point_helper="TMAP_Plot")
    faerun.plot(file_name='TMAP', path=args.output, template="smiles")


if __name__ == '__main__':
    main()