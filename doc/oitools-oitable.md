# OiTools OiTable

![](./svg/oitools-oitable.svg)

The `FitsHDU` contains the keywords.

The `FitsTable` (also abstract) contains the descriptions of the columns of a [Fits binary table](https://archive.stsci.edu/fits/users_guide/node44.html#SECTION00560000000000000000).

The `OITable` references the parent `OIFitsFile`. Indeed an `OITable` belongs to exactly one `OIFitsFile`, and an `OIFitsFile` contains several `OITable`.

The `OICorr`, `OIWavelength`, `OIArray`, and `OITarget` are concrete final classes containing the different binary tables in OI. Each one define its own keywords.