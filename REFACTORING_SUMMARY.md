# Preference API Refactoring Summary

## Overview
This refactoring standardized all JavaScript modules in the OpenRefine project to use `OpenRefine.getPreference()` and `OpenRefine.setPreference()` throughout, replacing direct AJAX calls and mixed usage patterns.

## Files Refactored

### Core Modules

#### 1. **clustering-dialog.js**
- **Changes:**
  - Replaced `Refine.getPreference()` with `thePreferences` lookup for synchronous access (3 instances)
  - Replaced `Refine.setPreference()` with `OpenRefine.setPreference()` (2 instances)
  - Replaced direct `$.ajax()` calls with `OpenRefine.getPreference()` using Promise.all() for parallel requests
  - Removed TODO comments about switching to standard API
- **Benefits:**
  - Parallel loading of custom keying and distance functions
  - Consistent use of cached preferences for synchronous access
  - Consistent use of async API for updates

#### 2. **clustering-functions-dialog.js** (previously completed)
- **Changes:**
  - Replaced all direct `$.ajax()` calls with `OpenRefine.getPreference()` and `OpenRefine.setPreference()`
  - Removed `Refine.wrapCSRF()` wrapper (now handled internally)
  - Eliminated FIXME comments about duplicate CSRF handling code
- **Benefits:**
  - Removed 3 instances of duplicate CSRF handling code
  - Cleaner promise-based async operations

#### 3. **expression-preview-dialog.js**
- **Changes:**
  - Replaced `Refine.getPreference()` with `thePreferences` lookup for choices limit
- **Benefits:**
  - Consistent synchronous access pattern

#### 4. **cell-ui.js**
- **Changes:**
  - Replaced direct `$.ajax()` call with `OpenRefine.getPreference()` in IIFE
- **Benefits:**
  - Standardized API usage for preference loading

#### 5. **lang-settings-ui.js**
- **Changes:**
  - Replaced direct `$.ajax()` call with `OpenRefine.setPreference()` in language selector
  - Removed `Refine.wrapCSRF()` wrapper
  - Removed `async: false` (now properly async)
- **Benefits:**
  - Cleaner async code with promises
  - Removed blocking synchronous AJAX call

#### 6. **index.js**
- **Changes:**
  - Replaced `Refine.postCSRF2()` with `OpenRefine.setPreference()` in `storeNotificationStatus()`
  - Replaced `$.get()` with `OpenRefine.getPreference()` in `showNotifications()`
- **Benefits:**
  - Consistent API usage throughout
  - Simplified function implementations

#### 7. **preferences.js**
- **Changes:**
  - Replaced `Refine.postCSRF2()` with `OpenRefine.setPreference()` in edit button handler
  - Replaced `Refine.postCSRF2()` with `OpenRefine.setPreference()` in delete button handler (passing null for deletion)
  - Replaced `Refine.postCSRF2()` with `OpenRefine.setPreference()` in add preference button
- **Benefits:**
  - Consistent API usage in preference management UI
  - Automatic JSON serialization handled by `OpenRefine.setPreference()`

#### 8. **list-facet.js**
- **Changes:**
  - Replaced `Refine.postCSRF2()` with `OpenRefine.setPreference()` in facet limit setting
- **Benefits:**
  - Standardized API usage

#### 9. **recon-manager.js**
- **Changes:**
  - Replaced direct `$.ajax()` call with `OpenRefine.setPreference()` in `save()` function
  - Replaced direct `$.ajax()` call with `OpenRefine.getPreference()` in initialization IIFE
  - Removed `Refine.wrapCSRF()` wrapper
  - Removed `async: false` (now properly async)
- **Benefits:**
  - Removed blocking synchronous AJAX calls
  - Cleaner promise-based initialization

#### 10. **templating-exporter-dialog.js**
- **Changes:**
  - Replaced `$.getJSON()` with `OpenRefine.getPreference()` in `_getSavedTemplate()`
- **Benefits:**
  - Consistent API usage

#### 11. **range-facet.js**
- **Changes:**
  - Replaced `Refine.getPreference()` with `thePreferences` lookup for user language
- **Benefits:**
  - Consistent synchronous access pattern

#### 12. **standard-service-panel.js**
- **Changes:**
  - Replaced `Refine.getPreference()` with `thePreferences` lookup for automatch setting
  - Simplified boolean check (no JSON.parse needed)
- **Benefits:**
  - Cleaner code with direct boolean access

#### 13. **data-table-view.js**
- **Changes:**
  - Replaced `Refine.getPreference()` with `thePreferences` lookup for page size
  - Added conditional JSON.parse only if value exists
- **Benefits:**
  - Consistent synchronous access pattern

#### 14. **menu-edit-cells.js**
- **Changes:**
  - Replaced `Refine.getPreference()` with `thePreferences` lookup in `doJoinMultiValueCells()`
  - Replaced `Refine.setPreference()` with `OpenRefine.setPreference()` in `doJoinMultiValueCells()`
  - Replaced `Refine.getPreference()` with `thePreferences` lookup in `doSplitMultiValueCells()`
  - Replaced `Refine.setPreference()` with `OpenRefine.setPreference()` in `doSplitMultiValueCells()`
- **Benefits:**
  - Consistent API usage for cell operations

### Extension Modules

#### 15. **wikibase-manager.js**
- **Changes:**
  - Replaced direct `$.ajax()` call with `OpenRefine.setPreference()` in `saveWikibases()`
  - Replaced direct `$.ajax()` call with `OpenRefine.getPreference()` in `loadWikibases()`
  - Removed `Refine.wrapCSRF()` wrapper
  - Removed `async: false` and `dataType` options
- **Benefits:**
  - Removed blocking synchronous AJAX calls
  - Automatic JSON serialization

#### 16. **template-manager.js**
- **Changes:**
  - Replaced direct `$.ajax()` call with `OpenRefine.getPreference()` in `loadTemplates()`
  - Replaced direct `$.ajax()` call with `OpenRefine.setPreference()` in `saveTemplates()`
  - Removed `Refine.wrapCSRF()` wrapper
  - Removed `async: false` and `dataType` options
- **Benefits:**
  - Removed blocking synchronous AJAX calls
  - Simplified save implementation

## Key Patterns

### Pattern 1: Synchronous Access (Using Cached Preferences)
**Use Case:** When preferences are needed synchronously during initialization or rendering.

**Before:**
```javascript
var value = Refine.getPreference("preference.key", defaultValue);
```

**After:**
```javascript
var value = thePreferences["preference.key"] || defaultValue;
```

### Pattern 2: Async Read (Promise-based)
**Use Case:** When loading preferences asynchronously.

**Before:**
```javascript
$.ajax({
    url: "command/core/get-preference?" + $.param({ name: "preference.key" }),
    success: function(data) {
        // use data.value
    },
    dataType: "json"
});
```

**After:**
```javascript
OpenRefine.getPreference("preference.key")
    .then(function(data) {
        // use data.value
    });
```

### Pattern 3: Async Write (Promise-based)
**Use Case:** When saving preferences.

**Before:**
```javascript
Refine.wrapCSRF(function(token) {
    $.ajax({
        type: "POST",
        url: "command/core/set-preference?" + $.param({ name: "preference.key" }),
        data: {
            value: JSON.stringify(newValue),
            csrf_token: token
        },
        dataType: "json"
    });
});
```

**After:**
```javascript
OpenRefine.setPreference("preference.key", newValue);
```

### Pattern 4: Parallel Async Reads
**Use Case:** When loading multiple preferences in parallel.

**Before:**
```javascript
$.ajax({ /* first preference */ });
$.ajax({ /* second preference */ });
```

**After:**
```javascript
Promise.all([
    OpenRefine.getPreference("first.preference"),
    OpenRefine.getPreference("second.preference")
]).then(function(results) {
    // use results[0] and results[1]
});
```

## Files NOT Changed

The following files were intentionally left unchanged:

1. **project.js** - Contains the synchronous wrapper `Refine.getPreference/setPreference` that uses cached `thePreferences`
2. **util/openrefine.js** - Contains the canonical async implementations of `OpenRefine.getPreference/setPreference`
3. **Test files** - Cypress tests and API test files should use direct API calls for testing purposes

## Benefits Summary

1. **Consistency**: All production code now uses standardized API methods
2. **Maintainability**: Removed duplicate CSRF handling code (eliminated 6+ instances)
3. **Performance**: Removed blocking synchronous AJAX calls (5+ instances)
4. **Simplicity**: Automatic JSON serialization handled by API
5. **Better Practices**: Promise-based async operations throughout
6. **Code Quality**: Removed TODO/FIXME comments that are now resolved

## Migration Notes

- The `thePreferences` global object is populated on page load and should be used for synchronous access
- `OpenRefine.setPreference()` automatically handles JSON serialization - no need to call `JSON.stringify()`
- `OpenRefine.setPreference()` automatically handles CSRF tokens - no need to call `Refine.wrapCSRF()`
- For deleting preferences, pass `null` as the value to `OpenRefine.setPreference()`
- All async operations now return promises for better error handling and chaining

## Date Completed
February 8, 2026
