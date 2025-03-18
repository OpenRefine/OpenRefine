/*

Copyright 2024, OpenRefine contributors
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
 * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

function addToMultiMap(multiMap, key, value) {
    let list = multiMap.get(key);
    if (list === undefined) {
        list = [];
        multiMap.set(key, list);
    }
    list.push(value);
}

/*
 Takes a JSON representation of operations as produced by the get-column-dependencies operation
 and draws a graphical representation of the operations as a SVG diagram.
 */
class RecipeVisualizer {
    
    constructor(analyzedOperations, svg) {
      this.analyzedOperations = analyzedOperations;
      this.hoverTimeout = null;
      this.tooltip = null;
      this.svg = svg;
      this.minWidth = 400;
      // helper to predict the width of text in labels. The canvas element isn't used except for that purpose.
      this.measuringContext = $('<canvas>')[0].getContext('2d');
      this.lengthCache = new Map();
    }

    // Predicts the width of a column label. This is a slight over-approximation.
    // This is used for the purposes of positioning the column labels in a compact way,
    // without overlapping.
    predictWidth(text) {
      if (!this.lengthCache.has(text)) {
        let metrics = this.measuringContext.measureText(text);
        let length = metrics.actualBoundingBoxRight + metrics.actualBoundingBoxLeft;
        this.lengthCache.set(text, length*1.2 + 30); // add a bit of margin for the left triangle, borders and padding around the text
      }
      return this.lengthCache.get(text);
    }

    draw() {
        let operationsWithIds = this.computeColumnIds();
        let columnPositions = this.computeColumnPositions(operationsWithIds);

        const svg = this.svg;
        let sliceHeight = 35;
        let columnDistance = 35;
        let columnHoverMargin = 10;
        let opaqueMargin = 5;
        let columnColor = '#888';
        let columnWidth = 2;
        let dependencyRadius = 5;
        let columnNameHeight = 25;

        // Define arrow marker
        let defs = $(document.createElementNS('http://www.w3.org/2000/svg', 'defs'))
          .appendTo(svg);
        let marker = $(document.createElementNS('http://www.w3.org/2000/svg', 'marker'))
          .attr('id', 'arrow')
          .attr('viewBox', '0 0 10 10')
          .attr('refX', '5')
          .attr('refY', '5')
          .attr('markerWidth', '4')
          .attr('markerHeight', '4')
          .attr('orient', 'auto-start-reverse')
          .appendTo(defs);
        let markerPath = $(document.createElementNS('http://www.w3.org/2000/svg', 'path'))
          .attr('d', 'M 0 0 L 10 5 L 0 10 z')
          .attr('fill', 'context-stroke')
          .appendTo(marker);

        // Compute diagram boundaries
        let maxX = columnDistance * 2;
        let inputColumns = [];
        let outputColumns = [];
        for (const column of operationsWithIds.columns) {
          let xPos = (columnPositions.get(column.id) + 1) * columnDistance;
          maxX = Math.max(maxX, xPos + columnDistance);
          if (column.start === 0) {
            inputColumns.push({xPos, column, name: column.names[0].name});
          }
          if (column.end === operationsWithIds.translatedOperations.length) {
            if (column.start !== 0 || column.names.length > 1) {
              outputColumns.push({xPos, column, name: column.names[column.names.length - 1].name});
            }
          }
        }
        let maxY = operationsWithIds.translatedOperations.length * sliceHeight;

        // Pre-compute the positions of column labels, without drawing them yet
        let inputColumnLabelAreaHeight = this.layoutBoundaryColumnNames(inputColumns, columnDistance, columnNameHeight);
        let outputColumnLabelAreaHeight = this.layoutBoundaryColumnNames(outputColumns, columnDistance, columnNameHeight);

        // Draw slices
        let boundaryMin = - columnDistance;
        let boundaryMax = maxX + 2 * columnDistance;
        if (boundaryMax - boundaryMin < this.minWidth) {
          boundaryMin -= (this.minWidth - boundaryMax + boundaryMin) / 2;
          boundaryMax += (this.minWidth - boundaryMax + boundaryMin) / 2;
        }
        for (let i = 0; i < operationsWithIds.translatedOperations.length; i += 2) {
          $(document.createElementNS('http://www.w3.org/2000/svg', 'rect'))
              .attr('x', boundaryMin)
              .attr('y', i * sliceHeight)
              .attr('width', boundaryMax - boundaryMin)
              .attr('height', sliceHeight)
              .attr('fill', '#f2f2f2')
              .appendTo(svg);
        }

        // Draw all column lines
        let edgesGroup = $(document.createElementNS('http://www.w3.org/2000/svg', 'g'))
          .appendTo(svg);
        for(const column of operationsWithIds.columns) {
            let xPos = (columnPositions.get(column.id) + 1) * columnDistance;
            for(const name of column.names) {
              let start = name.start;
              let end = name.end === undefined ? column.end : name.end;
              let y1 = (start - 0.5) * sliceHeight;
              let y2 = (end + 0.5) * sliceHeight;
              if (start === 0) {
                y1 = -1 * inputColumnLabelAreaHeight;
              }
              if (end === operationsWithIds.translatedOperations.length) {
                y2 += outputColumnLabelAreaHeight;
              }
              let line = $(document.createElementNS('http://www.w3.org/2000/svg', 'line'))
                .attr('id', `column-${column.id}-${start}`)
                .attr('x1', xPos)
                .attr('y1', y1)
                .attr('x2', xPos)
                .attr('y2', y2)
                .attr("stroke", columnColor)
                .attr("stroke-width", columnWidth)
                .attr("alt", name.name)
                .appendTo(edgesGroup);
              let hoverArea = $(document.createElementNS('http://www.w3.org/2000/svg', 'rect'))
                .attr('x', xPos - columnHoverMargin)
                .attr('y', (start - 0.5) * sliceHeight)
                .attr('width', columnHoverMargin * 2)
                .attr('height', (end - start + 1)*sliceHeight)
                .attr('fill', 'white')
                .attr('fill-opacity', 0)
                .appendTo(svg);
              this.setUpTooltip(svg, hoverArea, line, 2, name.name, true);
              if (end === operationsWithIds.translatedOperations.length) {
                line.attr('marker-end', 'url(#arrow)');
              }
            }
        }

        // Draw input and output column names
        this.drawBoundaryColumnNames(svg, inputColumns, -1 * inputColumnLabelAreaHeight, columnNameHeight);
        this.drawBoundaryColumnNames(svg, outputColumns, (operationsWithIds.translatedOperations.length + 0.5) * sliceHeight, columnNameHeight);

        // Draw all operations
        let sliceId = 0;
        for(const slice of operationsWithIds.translatedOperations) {
          if (slice.operation.columnsDiff == null) {
            let posX = opaqueMargin;
            let posY = sliceId * sliceHeight + opaqueMargin;
            let width = maxX - 2*opaqueMargin;
            let height = sliceHeight - 2*opaqueMargin;
            let rect = $(document.createElementNS('http://www.w3.org/2000/svg', 'rect'))
              .attr('id', `opaque-${sliceId}`)
              .attr('x', posX)
              .attr('y', posY)
              .attr('width', width)
              .attr('height', height)
              .attr('stroke', 'black')
              .attr('stroke-width', 1)
              .attr('fill', 'white')
              .appendTo(svg);
            this.addOperationLogo(svg, maxX / 2, (sliceId + 0.5) * sliceHeight, slice.operation.operation, sliceId, false);
            let hoverArea = $(document.createElementNS('http://www.w3.org/2000/svg', 'rect'))
              .attr('id', `opaque-${sliceId}`)
              .attr('x', posX)
              .attr('y', posY)
              .attr('width', width)
              .attr('height', height)
              .attr('fill', 'white')
              .attr('fill-opacity', 0)
              .appendTo(svg);
            this.setUpTooltip(svg, hoverArea, rect, 10, slice.operation.operation.description, false);
          } else {
            // Draw operation circles for added columns
            for (const addedColumn of slice.columnIds.added) {
              let columnId = addedColumn.id;
              let xPos = (columnPositions.get(columnId) + 1) * columnDistance;
              let yPos = (sliceId + 0.5)* sliceHeight;
              if (addedColumn.afterId !== undefined) {
                let xPosSource = (columnPositions.get(addedColumn.afterId) + 1) * columnDistance;
                var line = $(document.createElementNS('http://www.w3.org/2000/svg', 'line'))
                  .attr('x1', xPosSource)
                  .attr('y1', yPos)
                  .attr('x2', xPos - 15)
                  .attr('y2', yPos)
                  .attr('stroke', columnColor)
                  .attr('stroke-width', columnWidth)
                  .attr('marker-end', 'url(#arrow)')
                  .appendTo(edgesGroup);
              }
              this.makeOperationCircle(svg, xPos, yPos, `circle-${sliceId}-${columnId}`, slice.operation.operation);
            }

            // Draw operation circles for modified and deleted columns
            for (const columnId of slice.columnIds.modified.concat(slice.columnIds.deleted)) {
              let xPos = (columnPositions.get(columnId) + 1) * columnDistance;
              let yPos = (sliceId + 0.5)* sliceHeight;
              this.makeOperationCircle(svg, xPos, yPos, `circle-${sliceId}-${columnId}`, slice.operation.operation);
            }

            // For operations that don't add, modify or delete any columns, draw one operation circle in the left margin
            if (slice.columnIds.added.length == 0 && slice.columnIds.modified.length == 0 && slice.columnIds.deleted.length == 0) {
              let xPos = 0;
              let yPos = (sliceId + 0.5)* sliceHeight;
              this.makeOperationCircle(svg, xPos, yPos, `circle-${sliceId}-default`, slice.operation.operation);
            }

            // Draw other column dependencies
            let modifiedOrDeleted = new Set(slice.columnIds.modified.concat(slice.columnIds.deleted));
            for (const columnId of slice.columnIds.dependencies || []) {
              if (!modifiedOrDeleted.has(columnId)) {
                let xPos = (columnPositions.get(columnId) + 1) * columnDistance;
                let yPos = (sliceId + 0.5) * sliceHeight;
                let circle = $(document.createElementNS('http://www.w3.org/2000/svg', 'circle'))
                  .attr('cx', xPos)
                  .attr('cy', yPos)
                  .attr('r', dependencyRadius)
                  .attr('fill', columnColor)
                  .appendTo(svg);
                this.setUpTooltip(svg, circle, circle, dependencyRadius, this.nameAtSlice(operationsWithIds.columns[columnId], sliceId), false);
              }
            }
          }
          sliceId++;
        }

        // Resize canvas to make it of a fitting size
        let bbox = svg[0].getBBox();
        let margin = 5;
        svg.attr("viewBox", `${bbox.x} ${bbox.y - margin} ${bbox.width} ${bbox.height + 2*margin}`);
        svg.attr("width", `${bbox.width}`);
    }

    nameAtSlice(column, sliceId) {
      for (let i = 0; i != column.names.length; i++) {
        if (column.names[i].end === undefined || sliceId <= column.names[i].end) {
          return column.names[i].name;
        }
      }
    }

    makeOperationCircle(svg, xPos, yPos, id, operation) {
      let circleRadius = 12;
      let circleHoverRadius = 15;
      let circle = $(document.createElementNS('http://www.w3.org/2000/svg', 'circle'))
        .attr('id', id)
        .attr('cx', xPos)
        .attr('cy', yPos)
        .attr('r', circleRadius)
        .attr('stroke', 'black')
        .attr('fill', 'white')
        .appendTo(svg);
      this.addOperationLogo(svg, xPos, yPos, operation, id, true);
      let hoverArea = $(document.createElementNS('http://www.w3.org/2000/svg', 'circle'))
        .attr('id', id)
        .attr('cx', xPos)
        .attr('cy', yPos)
        .attr('r', circleHoverRadius)
        .attr('opacity', 0)
        .attr('fill', 'white')
        .appendTo(svg);
      this.setUpTooltip(svg, hoverArea, circle, circleHoverRadius, operation.description, false);
      return circle;
    }

    addOperationLogo(svg, xPos, yPos, operation, id, clipToCircle) {
      let operationIcon = OperationIconRegistry.getIcon(operation.op);
      if (operationIcon === undefined) {
        let firstLetter = operation.description[0];
        $(document.createElementNS('http://www.w3.org/2000/svg', 'text'))
          .attr('x', xPos)
          .attr('y', yPos)
          .attr('dominant-baseline', 'middle')
          .attr('text-anchor', 'middle')
          .text(firstLetter)
          .appendTo(svg);
      } else {
        let imageRadius = 10;
        let clipId = undefined;
        if (clipToCircle) {
          clipId = `icon-clip-${id}`;
          let clipPath = $(document.createElementNS('http://www.w3.org/2000/svg', 'clipPath'))
            .attr('id', clipId)
            .appendTo(svg);
          $(document.createElementNS('http://www.w3.org/2000/svg', 'circle'))
            .attr('id', id)
            .attr('cx', xPos)
            .attr('cy', yPos)
            .attr('r', imageRadius)
            .appendTo(clipPath);
        }
        let image = $(document.createElementNS('http://www.w3.org/2000/svg', 'image'))
          .attr('x', xPos - imageRadius)
          .attr('y', yPos - imageRadius)
          .attr('href', operationIcon)
          .attr('width', imageRadius * 2)
          .appendTo(svg);
        if (clipId !== undefined) {
          image.attr('clip-path', `url(#${clipId})`);
        }
      }
    }

    setUpTooltip(svg, hoverArea, relativeTo, tooltipOffset, text, useCursorY) {
      let self = this;
      hoverArea.on("mouseenter", function(event) {
        if (self.hoverTimeout != null) {
          clearTimeout(self.hoverTimeout);
          self.hoverTimeout = null;
        }
        self.hoverTimeout = setTimeout(function() {
          self.hoverTimeout = null;
          if (self.tooltip != null) {
            self.tooltip.remove();
            self.tooltip = null;
          }
          let rect = relativeTo[0].getBoundingClientRect();
          let x = (rect.left + rect.right) / 2;
          let y = (rect.top + rect.bottom) / 2;
          if (useCursorY) {
            y = event.clientY;
          }
          self.tooltip = $("<div></div>")
            .css("left", x + tooltipOffset)
            .addClass('recipe-tooltip')
            .appendTo(svg.parent());
          $("<div></div>")
            .addClass('recipe-tooltip-triangle')
            .appendTo(self.tooltip);
          $("<div></div>")
            .text(text)
            .addClass('recipe-tooltip-inner')
            .appendTo(self.tooltip);
          self.tooltip.css("top", y - 0.5 * self.tooltip.outerHeight());
        }, 300);

        hoverArea.on("mouseleave", function() {
          if (self.hoverTimeout != null) {
            clearTimeout(self.hoverTimeout);
            self.hoverTimeout = null;
          }
          if (self.tooltip != null) {
            self.tooltip.remove();
            self.tooltip = null;
          }
        });
      });
    }

    // Pre-computes the vertical positions of static column labels, for input and output columns.
    // Returns the height of the corresponding area
    layoutBoundaryColumnNames(columns, columnDistance, columnNameHeight) {
      columns.sort((a, b) => a.xPos - b.xPos);

      let currentHeight = 0; // height at which the next column label can be positioned
      let minTopPosition = 0; // minimum horizontal position for a column label to be rendered at height=0
      let maxHeight = 0; // maximum height of all column labels encountered in the list

      for (const column of columns) {
        column.labelHeight = 0;
        if (column.xPos >= minTopPosition) { // the column is sufficiently far on the right to be positioned at height=0 again
          currentHeight = 0;
        } else { // we stack the column on its own line, because it would otherwise collide with labels laid out earlier
          currentHeight += columnNameHeight;
          column.labelHeight = currentHeight;
        }

        let width = this.predictWidth(column.name);
        minTopPosition = Math.max(minTopPosition, column.xPos + width - columnDistance * (currentHeight / columnNameHeight));
        maxHeight = Math.max(currentHeight + columnNameHeight * 1.5, maxHeight);
      }
      return maxHeight;
    }

    // Render the static column labels for input and output columns.
    // This assumes that the vertical positions of the labels have been pre-computed
    // in layoutBoundaryColumnNames
    drawBoundaryColumnNames(svg, columns, yOffset, columnNameHeight) {
      columns.sort((a, b) => a.xPos - b.xPos);
      let i = 0;
      for (const column of columns) {
        let width = this.predictWidth(column.name);
        let g = $(document.createElementNS('http://www.w3.org/2000/svg', 'g'))
        .appendTo(svg);
        let fo = $(document.createElementNS('http://www.w3.org/2000/svg', 'foreignObject'))
          .attr('x', column.xPos)
          .attr('y', yOffset + column.labelHeight)
          .width(width)
          .height(columnNameHeight)
          .appendTo(g);
        let div = $(document.createElement('div'))
          .addClass('recipe-tooltip')
          .appendTo(fo);
        $(document.createElement('div'))
          .addClass('recipe-tooltip-triangle')
          .appendTo(div);
        $(document.createElement('div'))
          .addClass('recipe-tooltip-inner-embedded')
          .text(column.name)
          .appendTo(div);
        i++;
      }
    }

    computeColumnPositions(operationsWithIds) {
        // Do a topological sort
        let unconstrainedColumns = new Set();
        let positionMap = new Map();
        for (let i = 0; i != operationsWithIds.columns.length; i++) {
            unconstrainedColumns.add(i);
            positionMap.set(i, 0);
        }
        // Index the constraints in bidirectional maps
        let leftToRight = new Map();
        let rightToLeft = new Map();
        for (const constraint of operationsWithIds.constraints) {
            addToMultiMap(leftToRight, constraint.left, constraint.right);
            addToMultiMap(rightToLeft, constraint.right, constraint.left);
            unconstrainedColumns.delete(constraint.right);
        }

        // main loop of the topological sort algorithm, greedily allocating
        // positions to columns which are no longer constrained to be to the right of any column
        let sorted = [];
        while (unconstrainedColumns.size > 0) {
            let id = unconstrainedColumns[Symbol.iterator]().next().value;
            unconstrainedColumns.delete(id);
            let position = positionMap.get(id);
            sorted.push(id);
            for(const rightId of leftToRight.get(id) || []) {
                positionMap.set(rightId, Math.max(positionMap.get(rightId), position + 1));
                let otherConstraints = rightToLeft.get(rightId);
                let newConstraints = otherConstraints.filter(x => x != id);
                rightToLeft.set(rightId, newConstraints);
                if (newConstraints.length == 0) {
                    unconstrainedColumns.add(rightId);
                }
            }
        }
        if (sorted.length < operationsWithIds.columns.length) {
            throw new Error("invalid column constraints: loop detected");
        }

        return positionMap;
    }
  
    computeColumnIds() {
      // map from names of columns currently present to their id
      let currentColumns = {};
      // list of ids of columns currently present, in the order they appear
      let currentColumnIds = [];
      // list of order constraints between column ids, of the form {left: 2, right: 5},
      // which means that the horizontal position of column 2 must be strictly less than that of column 5
      let constraints = [];
      // position of the slice where all columns were last reset (= position of the last opaque slice)
      let lastColumnReset = 0;
      // set of the rightmost columns since the columns were last reset
      let rightmostSinceReset = new Set();
      // list of column metadata (indices in this list are column ids)
      let columns = [];
      // operations translated to have their column dependencies as column ids
      let translatedOperations = [];
      for (let i = 0; i != this.analyzedOperations.length; i++) {
        let operation = this.analyzedOperations[i];

        let columnIds = {
        };

        columnIds.dependencies = [];
        var fullDependencies = new Set(operation.dependencies || []);
        if (operation.columnsDiff != null) {
          for (let impliedDependency of operation.columnsDiff.impliedDependencies) {
            fullDependencies = fullDependencies.add(impliedDependency);
          }
        }
        for(const columnName of fullDependencies) {
            if (currentColumns[columnName] === undefined) {
                let columnId = columns.length;
                currentColumns[columnName] = columnId;
                currentColumnIds.push(columnId);
                if (currentColumnIds.length > 1) {
                    constraints.push({left: currentColumnIds[currentColumnIds.length - 2], right: columnId});
                }
                // make sure this column appears to the right of all of the previous rightmost columns
                for (let rightmost of rightmostSinceReset) {
                  constraints.push({left: rightmost, right: columnId});
                }
                rightmostSinceReset = new Set([ columnId ]);

                columns.push({
                    id: columnId,
                    start: lastColumnReset,
                    names: [{
                      name: columnName,
                      start: lastColumnReset
                    }]
                })
            }
            columnIds.dependencies.push(currentColumns[columnName]);
        }

        if (operation.columnsDiff === null) {
            // make all current columns end here
            for(const [name, id] of Object.entries(currentColumns)) {
                columns[id].end = i;
            }
            currentColumns = {};
            currentColumnIds = [];
            lastColumnReset = i + 1;
            rightmostSinceReset = new Set();
        } else {
            columnIds.added = [];
            columnIds.deleted = [];
            columnIds.modified = [];
            for(const deletedColumn of operation.columnsDiff.deleted) {
                let id = currentColumns[deletedColumn];
                if (id !== undefined) {
                    columnIds.deleted.push(id);
                    // check if this column removal is part of a rename,
                    // by checking if any column was added based on this column in the current slice
                    let addedBasedOnThisColumn = operation.columnsDiff.added.find(added => added.afterName == deletedColumn);
                    if (addedBasedOnThisColumn == undefined) {
                      columns[id].end = i;
                      delete currentColumns[deletedColumn];
                      currentColumnIds = currentColumnIds.filter(x => x != id);
                    } else {
                      currentColumns[addedBasedOnThisColumn.name] = id;
                      addedBasedOnThisColumn.isRename = true;
                      let names = columns[id].names;
                      names[names.length - 1].end = i;
                      names.push({
                        name: addedBasedOnThisColumn.name,
                        start: i + 1
                      });
                    }
                }
            }
            for (const addedColumn of operation.columnsDiff.added) {
                // TODO check that it does not collide with existing column names
                if (addedColumn.isRename) {
                  continue;
                }
                let name = addedColumn.name;
                let columnId = columns.length;
                currentColumns[name] = columnId;
                columns.push({
                    id: columnId,
                    start: i + 1,
                    names: [{
                      name,
                      start: i + 1
                    }]
                });
                let addedColumnAsId = {
                    id: columnId
                };
                if (addedColumn.afterName != null) {
                    let afterId = currentColumns[addedColumn.afterName];
                    addedColumnAsId.afterId = afterId;
                    let indexOfAfterId = currentColumnIds.indexOf(afterId);
                    if (indexOfAfterId == -1) {
                        indexOfAfterId = currentColumnIds.length - 1;
                    } else {
                        constraints.push({left: afterId, right: columnId});
                    }
                    currentColumnIds.splice(indexOfAfterId + 1, 0, columnId);
                    if (indexOfAfterId + 2 < currentColumnIds.length) {
                        constraints.push({left: columnId, right: currentColumnIds[indexOfAfterId + 2]});
                    }
                } else {
                    // if no afterName is provided, the column gets added at the end of the list
                    currentColumnIds.push(columnId);
                    if (currentColumnIds.length > 1) {
                        constraints.push({left: currentColumnIds[currentColumnIds.length - 2], right: columnId});
                    }
                }
                columnIds.added.push(addedColumnAsId);
            }
            for (const modifiedColumn of operation.columnsDiff.modified) {
                let id = currentColumns[modifiedColumn];
                if (id !== undefined) {
                    columnIds.modified.push(id);
                }
            }

            if (currentColumnIds.length > 0) {
              rightmostSinceReset.add(currentColumnIds[currentColumnIds.length - 1]);
            }
        }

        translatedOperations.push({
            operation,
            columnIds,
        });
      }
      // fill the end field on all columns that remain at the end
      for(let column of columns) {
        if (column.end === undefined) {
            column.end = this.analyzedOperations.length;
        }
      }
      return {
        columns,
        constraints,
        translatedOperations,
      };
    }
  
  }