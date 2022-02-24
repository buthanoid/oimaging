# OImaging Structures

In this document we go through some of the most important Java structures used in OImaging. The goal is to be able to discuss OImaging development without needing to dive in the source code.

![](./svg/oimaging-structures.svg)

## FitsImageHDU

In a lot of cases it is correct to call this structure "an image".

A `FitsImageHDU` is a *Fits HDU* with *Header* and *Data* sections, with *image extension* (see [Fits guide](https://archive.stsci.edu/fits/users_guide/node41.html#SECTION00550000000000000000)).

It contains a list of `FitsImage`. This list often has only one element, that is why we call `FitsImageHDU` an "image". See section [`FitsImage`](#fitsimage) for more information about these.

It contains keywords, see section [`FitsHDU`](#fitshdu), since `FitsImageHDU` herits from it. An example is the keyword `HDU_NAME`.

Location: OiTools

## FitsHDU

This structure contains keywords and header cards. The difference between these two is that keywords have a description, see class `KeywordMeta`.

A keyword is identified by a `String` name. Its value can take one of the following types: `String`, `Double`, `Integer`, `Boolean`.

A header card has a `String` name and a `String` value. In practice the `String` value can be parsed as another type, but then you are supposed to declare it as a keyword.

Location: OiTools

## FitsImage

Contains the data of an image, by a two dimensions array of type float (4 octets).

Contains the meta-data of the image, by several fields, for example `incCol`, of type `double`, which stores the absolute coordinate increment along the column axis, in the metric radians by pixel.

You should note that this meta-data contains duplicate information with the keywords of the parent `FitsImageHDU`.
For example, the keyword `CRPIX1` and the field `pixRefCol` must always be equals.

A `FitsImage` always belong to exactly one `FitsImageHDU`.

Location: OiTools

<style>body { max-width: 1000px; } img { max-width: 100%; }</style>
