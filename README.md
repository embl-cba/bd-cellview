# BD Vulcan Image Processing

## Installation

- Download a fresh Fiji
- Copy the jars from `/g/cba/exchange/imflow/jars/` to the `Fiji.app/jars` folder

### Folder Structure 

The folder structure must be as shown below:

https://user-images.githubusercontent.com/2157566/93058267-71d7eb00-f66f-11ea-9254-0e4ec986931c.png

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
