package com.google.refine.importers.parsers;


public enum TreeParserToken {
    Ignorable,
    StartDocument,
    EndDocument,
    StartEntity,
    EndEntity,
    Value
    //append additional tokens as necessary (most are just mapped to Value or Ignorable)
}
