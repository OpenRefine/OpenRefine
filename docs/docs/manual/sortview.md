---
id: sortview
title: Sort and view
sidebar_label: Sort and view
---

## Sort

You can temporarily sort your rows by one column. You can sort:
*   text alphabetically or reverse
*   numbers by largest or smallest
*   dates by earliest or latest
*   boolean values by false first or true first

You can also choose where to place errors and blank cells in the sorting. Text can be case-sensitive or not: cells that start with lowercase characters will appear ahead of uppercase.

![A screenshot of the Sort window.](/img/sort.png)

After you apply a sorting method, you can make it permanent, remove it, reverse it, or apply a subsequent sorting. You’ll find “Sort” in the project grid header to the right of the rows-display setting, which will show all current sorting settings. 

If you have multiple sorting methods applied, they will work in the order you applied them (represented in order in the "Sort" menu). For example, you can sort an "authors" column alphabetically, and then sort books by publication date, for those authors that have more than one book. If you apply those in a different order -  sort all the publication dates in the dataset first, and then alphabetically by author - your dataset will look different. 

![Temporarily sorted rows.](/img/sort2.png) 

When the sorting method you've applied is temporary, you will see that the rows retain their original numbering. When you make that sorting method permanent, by selecting "Reorder rows permanently," the row numbers will change and the "Sort" menu in the project grid header will disappear. This will apply all current sorting methods. 

## View

You can control what data you view in the grid. On each column, you can “collapse” that specific column, all other columns, all columns to the left, and all columns to the right. Using the “All” column’s dropdown menu, you can collapse all columns, and expand all the columns that you previously collapsed.

### Show/hide “null” 

You can also use the “All” dropdown to show and hide [“null” values](#cell-data-types). A small grey “null” will appear in each applicable cell. 

![A screenshot of what a null value looks like.](/img/null.png)
