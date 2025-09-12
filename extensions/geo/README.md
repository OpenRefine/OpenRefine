# OpenRefine GEO Extension

This extension adds geographic functions to OpenRefine for coordinate conversion and distance calculations.

## Functions

### `decToGMS(decimal, coordType)`

Converts decimal degrees to degrees, minutes, seconds format.

**Parameters:**
- `decimal` (number): Decimal degrees coordinate
- `coordType` (string, optional): 'lat', 'lng', or 'lon' for proper directional indicators

**Returns:** String in DMS format (e.g., "40° 42' 46.08\" N")

**Examples:**
```grel
decToGMS(40.7128)                    // "40° 42' 46.08\""
decToGMS(40.7128, "lat")             // "40° 42' 46.08\" N"
decToGMS(-74.0060, "lng")            // "74° 0' 21.60\" W"
decToGMS(-33.8688, "lat")            // "33° 52' 7.68\" S"
```

### `geoDistance(lat1, lng1, lat2, lng2, unit)`

Calculates the great circle distance between two coordinate pairs using the Haversine formula.

**Parameters:**
- `lat1` (number): First point latitude (-90 to 90)
- `lng1` (number): First point longitude (-180 to 180)
- `lat2` (number): Second point latitude (-90 to 90)
- `lng2` (number): Second point longitude (-180 to 180)
- `unit` (string, optional): 'm' (meters, default), 'km' (kilometers), or 'mi' (miles)

**Returns:** Number representing distance in specified unit

**Examples:**
```grel
geoDistance(40.7128, -74.0060, 34.0522, -118.2437)         // Distance in meters
geoDistance(40.7128, -74.0060, 34.0522, -118.2437, "km")   // Distance in kilometers
geoDistance(40.7128, -74.0060, 34.0522, -118.2437, "mi")   // Distance in miles
```

## Installation

This extension is built as part of the OpenRefine build process. The functions are automatically registered when OpenRefine starts.

## Implementation Notes

- Uses the Haversine formula for accurate great circle distance calculations
- Validates coordinate ranges (latitude: -90 to 90, longitude: -180 to 180)
- Handles edge cases like poles and international date line
- Comprehensive error handling with descriptive error messages

## Contributing

This extension was created in response to GitHub issue #6570. For bug reports or feature requests, please use the main OpenRefine issue tracker.

## License

This extension is licensed under the same BSD license as OpenRefine.