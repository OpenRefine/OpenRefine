<?php

/*
TODO
- Tons of content missing!
- Migrate all Google Code wiki content to the site
- Test in Internet Explorer
- Deploy on neologism.deri.ie
- Layout for Showcase page
- Better screenshot for homepage
- Improve screenshot display on homepage
- RDFa markup
*/

// ============= SITE STRUCTURE AND PAGES  ==========================================

$main_links = array(
    'Home' => '',
    'Showcases' => 'showcases',
    'Docs' => 'docs',
    'Support & Devel' => 'support-dev',
);

$section_links = array(
    'docs' => array(
        'Overview' => 'docs',
        'Installation guide' => 'installationDocs',
		'RDF export' => 'rdfExportDocs',
        'Reconciliation' => 'reconciliationDocs',
		'Standard SPARQL reconciliation' => 'sparqlReconDocs',
		'SPARQL with full-text search reconciliation' => 'searchReconDocs',
		'Using Sindice' => 'sindiceDocs',
        'Publications' => 'publications',
	'External resources' => 'extResources',
		'FAQ' => 'faq'
    ),
    'support-dev' => array(
        'Issue tracker' => 'issues',
        'Source code' => 'source',
    ),

    'showcases' => array(
		'Overview' => 'showcases',
        'Countries against DBpedia' => 'sparqlRecon',
        'Universities against The New York Times data' => 'dumpRecon',
        'DERI researchers using Sindice' => 'sindiceRecon',
		'RDF Export' => 'rdfExport',
    ),

);

$pages = array(
    '' => array(
        'head_title' => 'GRefine RDF Extension',
        'title_content' => 'home-intro',
        'content' => 'home-text',
    ),
    'showcases' => array(
        'title' => 'Showcases',
        'text' => 'showcases',
    ),
    'docs' => array(
        'title' => 'Documentation',
        'text' => 'docs',
    ),
    'support-dev' => array(
        'title' => 'Support and Development',
        'text' => 'support-dev',
        'sidebar' => 'team',
    ),
    'requirements' => array(
        'title' => 'Minimum Requirements',
        'section' => 'docs',
        'text' => 'requirements',
    ),
    'installationDocs' => array(
        'title' => 'Installation Guide',
        'section' => 'docs',
        'text' => 'installation',
    ),
    'rdfExportDocs' => array(
        'title' => 'RDF Export',
        'section' => 'docs',
        'text' => 'rdf-export-docs',
    ),
    'reconciliationDocs' => array(
        'title' => 'RDF Reconciliation',
        'section' => 'docs',
        'text' => 'reconciliation-docs',
    ),
	'sparqlReconDocs' => array(
		'title' => 'SPARQL-based Reconciliation',
		'section' => 'docs',
		'text' => 'sparql-recon-doc'
	),
	'searchReconDocs' => array(
		'title' => 'SPARQL with fulltext search -based Reconciliation',
		'section' => 'docs',
		'text' => 'search-recon-doc'
	),
	'sindiceDocs' => array(
        'title' => 'Using Sindice for reconciliation',
        'section' => 'docs',
        'text' => 'sindice-docs',
    ),
	'sparqlRecon' => array(
        'title' => 'Reconciling countries GDP against DBpedia',
        'section' => 'showcases',
        'text' => 'sparql-recon',
    ),
	'dumpRecon' => array(
        'title' => 'Reconciling the top universities against The New York Times data',
        'section' => 'showcases',
        'text' => 'dump-recon',
    ),
	'sindiceRecon' => array(
        'title' => 'Reconciling DERI researchers using Sindice',
        'section' => 'showcases',
        'text' => 'sindice-recon',
    ),
	'rdfExport' => array(
        'title' => 'RDF export',
        'section' => 'showcases',
        'text' => 'rdf-export',
    ),
	
    'extResources' => array(
        'title' => 'External Resources',
        'section' => 'docs',
        'text' => 'extResources',
    ),
    'faq' => array(
        'title' => 'Frequently Asked Questions',
        'section' => 'docs',
        'text' => 'faq',
    ),
    'publications' => array(
        'title' => 'Academic Publications',
        'section' => 'docs',
        'text' => 'publications',
    ),
    'gpl' => array('redirect' => 'http://www.gnu.org/licenses/gpl-3.0-standalone.html'),
    'download' => array('redirect' => 'http://code.google.com/p/neologism/downloads/list'),
    'download-latest' => array('redirect' => 'http://neologism.googlecode.com/files/neologism-0.4.10.zip'),
    'issues' => array('redirect' => 'https://github.com/fadmaa/grefine-rdf-extension/issues'),
    'source' => array('redirect' => 'https://github.com/fadmaa/grefine-rdf-extension'),
);

// ============= BEHAVIOUR CODE ==========================================

$uri = $_SERVER['REQUEST_URI'];
$path = str_replace('index.php', '', $_SERVER['SCRIPT_NAME']);
$absolute_base = 'http://' . $_SERVER['HTTP_HOST'] . $path;

if (substr($uri, 0, strlen($path)) == $path) {
    $uri = substr($uri, strlen($path));
}
if (!$uri) {
    $uri = '';
}
if (!isset($pages[$uri])) {
    $section = 'none';
    $title = '404 Not Found';
    $content = '404';
    header("HTTP/1.0 404 Not Found");
} else if (isset($pages[$uri]['redirect'])) {
    $section = 'none';
    $target = $pages[$uri]['redirect'];
    $title = 'Redirect';
    $text_html = '<p>The requested content is found at the following location: <a href="'
            . htmlspecialchars($target) . '">' . htmlspecialchars($target) . '</a></p>';
    header("Location: $target");
    header("HTTP/1.0 302 Found");
} else {
    $section = $uri;
    foreach ($pages[$uri] as $varname => $var) {
        $$varname = $var;
    }
    $page_links = @$section_links[$section];
}

// ============= TEMPLATE FUNCTIONS ==========================================

function e($s) { echo htmlspecialchars($s); }
function content($file) { readfile("content/$file.html"); }

// ============= BEGIN TEMPLATE ==============================================

echo '<?xml version="1.0" encoding="UTF-8"?>
';
?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML+RDFa 1.0//EN"
    "http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
  <head>
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
    <title><?php e(@$head_title ? $head_title : ($title . ' | GRefine RDF Extension')); ?></title>
    <base href="<?php e($absolute_base); ?>" />
    <link rel="stylesheet" type="text/css" href="style.css" />
    <link rel="stylesheet" type="text/css" href="grefine-stuff.css" />	
    <link rel="shortcut icon" type="image/png" href="images/favicon.png" />
  </head>
  <body>
    <div id="header">
      <<?php e(($uri == '') ? 'h1' : 'div'); ?> id="logo">RDF Extension for Google Refine</<?php e(($uri == '') ? 'h1' : 'div'); ?>>
      <ul id="main-nav">
<?php
foreach ($main_links as $label => $link) {
    if ($section == $link) {
?>
        <li class="active"><span><?php e($label); ?></span></li>
<?php
    } else {
?>
        <li><a href="<?php e($link); ?>"><?php e($label); ?></a></li>
<?php
    }
}
?>
        <li class="download"><a href="download">Download</a></li>
      </ul>
    </div>
    <div id="title-stripe">
      <div id="title">
<?php
if (@$title) {
    ?><h1><?php e($title); ?></h1><?php
} else if (@$title_content) {
    content($title_content);
} else {
    echo "<h1>No title</h1>";
}
?>
      </div>
    </div>

    <div id="content">
<?php if (@$page_links) { ?>
      <ul id="section-nav">
<?php
foreach($page_links as $label => $link) {
    if ($link == $uri) { ?>
        <li><?php e($label); ?></li>
<?php } else { ?>
        <li><a href="<?php e($link); ?>"><?php e($label); ?></a></li>
<?php
    }
}
?>
      </ul>
<?php
}
if (@$sidebar) {
    ?><div id="sidebar"><?php content($sidebar); ?></div><?php
}
if (@$content) {
    content($content);
} else if (@$text) {
    ?><div id="main"<?php if (@$page_links) { ?> class="centrecol"<?php } ?>><?php content($text); ?></div><?php
} else if (@$text_html) {
    echo $text_html;
} else {
    echo "<p>No content or text configured for this page.</p>";
}
?>
    </div>

    <div id="footer">
      <div id="footer-text">
        <ul id="footer-links">
          <li><a href="about">About</a></li>
          <li><a href="http://linkeddata.deri.ie/">Linked Data Research Centre</a></li>
          <li><a href="http://deri.ie/">DERI</a></li>
          <li><a href="http://www.nuigalway.ie/">National University of Ireland, Galway</a></li>
          <li class="last"><a href="gpl">GPL</a></li>
        </ul>
        <div id="tagline">Friendly Linked Data publishing</div>
      </div>
    </div>
<script type="text/javascript">
var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");
document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));
</script>
<script type="text/javascript">
try {
var pageTracker = _gat._getTracker("UA-12554587-1");
pageTracker._trackPageview();
} catch(err) {}</script>
  </body>
</html>
