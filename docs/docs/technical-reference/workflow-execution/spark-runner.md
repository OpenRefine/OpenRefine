---
id: spark-runner
title: Spark runner
sidebar_label: Spark runner
---


The Spark runner uses [Apache Spark](https://spark.apache.org/) to execute workflows. It relies on the [RDD API](https://spark.apache.org/docs/latest/rdd-programming-guide.html), not the higher-level SQL API, for the following reasons:
- the SQL API puts some restrictions on the datatypes stored in cells, making it harder to store reconciliation data in cells directly;
- OpenRefine workflows heavily rely on expression languages, which would act as unanalyzable code blocks for the SQL engine, which would not be able to optimize SQL execution sufficiently
- many of OpenRefine's operations are not easy to translate to efficient SQL queries. For instance transpose operations or anything which relies on the records mode. The popularity of the tool is probably inherited from this different data model.
