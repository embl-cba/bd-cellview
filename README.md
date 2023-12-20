# BD CellView Image Processing

## Installation

1. Please [install Fiji](https://fiji.sc) on your computer.
2. Restart Fiji and install the BD CellView update site ([how to install an update site](https://imagej.net/Following_an_update_site#Introduction)).
    - [X] `BD CellView`
3. Restart Fiji

## Publication and usage instructions

Please see the supplemental material in: Schraivogel, D. et al. High-speed fluorescence image-enabled cell sorting. Science 375, 315-320 (2022). doi: [10.1126/science.abj3013](https://www.science.org/doi/10.1126/science.abj3013).

### Folder Structure 

The folder structure must be as shown below:

<img src="https://user-images.githubusercontent.com/2157566/93058267-71d7eb00-f66f-11ea-9254-0e4ec986931c.png">

### Available Channel Colors

Please see the Java AWT [Color specifications](https://docs.oracle.com/javase/7/docs/api/java/awt/Color.html). 

### Channel indices

The ICS generates multichannel TIFFs with 8 channels. You can select those channels 1-8 in the channel indices field of the Fiji Plugin. Here's an overview of different channels.

<img width="372" alt="Screen Shot 2022-03-03 at 19 23 59" src="https://user-images.githubusercontent.com/17741956/156627967-10fa87dc-4933-4cd9-bd46-27c215081488.png">

### Table

The table must look like this (the column name of `gate` may be changed as this can be specified in the user interface):

```
id;gate;path
id0;G2M;../images/Cell_Cycle_00000000.tiff
id1;G2M;../images/Cell_Cycle_00000001.tiff
id2;G2M;../images/Cell_Cycle_00000002.tiff
id3;G1;../images/Cell_Cycle_00000003.tiff
id4;G1;../images/Cell_Cycle_00000004.tiff
id5;G1;../images/Cell_Cycle_00011082.tiff
id6;S;../images/Cell_Cycle_00013268.tiff
id7;S;../images/Cell_Cycle_00014714.tiff
id8;S;../images/Cell_Cycle_00016839.tiff
id9;NA;../images/Cell_Cycle_00017276.tiff
```
