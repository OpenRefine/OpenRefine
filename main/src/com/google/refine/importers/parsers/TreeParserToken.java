package com.google.refine.importers.parsers;


public enum TreeParserToken {
    Ignorable,
    StartEntity,
    EndEntity,
    Value
    //append additional tokens only if necessary (most should be just mapped to Value or Ignorable)
}
