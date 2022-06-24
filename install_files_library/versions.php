<?php

set_time_limit(0);
ob_implicit_flush();

if ($argc < 2 || $argc > 3) { ?>
Process the manual to do some replacements.

  Usage:
  <?php echo $argv[0]; ?> <apply-script> [<startdir>]

  <apply-script> must contain the function apply($input),
  which recieves a whole xml-file, and should return
  the new file, or false if no modification needed.
  Apply scripts reside in the apply folder below this
  script. You only need to give the file name.

  With <startdir> you can specify in which dir
  to start looking recursively for xml files.

  Written by jeroen@php.net

<?php
    exit;
}

echo "Starting with manual-process\n";
echo "Including $argv[1]...";
include("apply/$argv[1]");
echo " done\n";
if (!function_exists('apply')) {
?>

### FATAL ERROR ###

In <?=$argv[1]?> you should define a function:
  string apply(string $string)

<?php
    exit;
}

$startdir = isset($argv[2]) ? $argv[2] : '.';

echo "Constructing list of all xml-files (may take a while)...";
$files = all_xml_files($startdir);
echo " done (".count($files)." xml files found)\n";

foreach ($files as $file) {

    echo "[Processing $file]\n";
    $fp = fopen($file,'r');
    $old = fread($fp,filesize($file));
    fclose($fp);

    if (!$old) {
        echo "WARNING: problem reading $file, skipping\n";
        continue;
    }

    $new = apply($old);
    if ($new === FALSE) { echo "NO MODIFICATION: $file not modified\n"; }
    else {
        $fp = fopen($file,'w');
        $res = fwrite($fp,$new);
        fclose($fp);
        if (!$res) {
            echo "WARNING: problem writing $file, file might be damaged\n";
            continue;
        }
    }
}

    
/* Utility functions: */

function all_xml_files($startdir)
{
    $startdir = ereg_replace('/+$','',$startdir);
    //echo "\$startdir = $startdir\n";
    $entries = array();

    $handle=opendir($startdir);
    while ($file = readdir($handle)) {

        $ffile = "$startdir/$file"; // full file(path)
        //echo "$file\n";
        if (ereg('\.xml$',$file))
            $entries[] = $ffile;
        if ($file[0] != '.' && is_dir($ffile))
            $entries = array_merge($entries,all_xml_files($ffile));
    }
    closedir($handle);

    return $entries;
   },
{
$php_src = "../../../php-src";

chdir(__DIR__ . "/$php_src");
$tags = array();
exec("git tag", $output);
foreach ($output as $tag) {
	if (preg_match('~^php-([0-9.]+)$~', $tag, $match)) {
		$tags[$tag] = $match[1];
	}
}
uksort($tags, 'version_compare');

$cache = __DIR__ . "/versions.ser";
if (file_exists($cache)) {
	$versions = unserialize(file_get_contents($cache));
} else {
	$versions = array(); // function => array(major => array(min, max))
	foreach ($tags as $tag => $version) {
		if ($version < 5) {
			continue;
		}
		echo "$tag: ";
		passthru("git checkout -q $tag", $return);
		if ($return) {
			continue;
		}
		$major = substr($version, 1, 1, 7);
		echo versions($versions, $major, $version) . " functions\n";
	}
	passthru("git checkout master");
	file_put_contents($cache, serialize($versions));
	echo "\n";
}

function versions(&$versions, $major, $version) {
	$aliases = array();
	$classes = array();
	$return = 0;
	foreach (rglob("*/") as $dirname) {
		$macros = array();
		$files = array();
		foreach (glob("$dirname*.[ch]*") as $filename) {
			$files[$filename] = $file = file_get_contents($filename);
			preg_match_all("~^#define[ \t]+(\\w+)(\\([^)]+\\))?([ \t]+.+[^\\\\])\$~msU", $file, $matches, PREG_SET_ORDER);
			foreach ($matches as $val) {
				$params = preg_split('~,\\s*~', trim($val[2], '()'));
				$macros[$val[1]] = array(trim(str_replace(array("\r", "\\\n"), "", $val[3])), $params);
			}
		}

		foreach ($files as $filename => $file) {
			// expand macros
			if ($macros && strlen(implode('|', array_keys($macros))) < 32000) {
				$files[$filename] = $file = preg_replace_callback('~\\b(' . implode('|', array_keys($macros)) . ')\\b(\\(.*\\))?~', function ($matches) use ($macros) {
					$macro = $macros[$matches[1]];
					if ($matches[2]) {
						$params = explode(",", trim($matches[2], '()'), count($macro[1]));
						return str_replace($macro[1], $params, $macro[0]);
					}
					return $macro[0];
				}, $file);
			}
			// named functions
			preg_match_all('~(?:PHP|ZEND)_NAMED_FE\\((\\w+)\\s*,\\s*(\\w+)~', $file, $matches, PREG_SET_ORDER);
			foreach ($matches as $match) {
				$aliases[$match[2]] = $match[1];
			}
			// methods
			preg_match_all('~INIT(?:_OVERLOADED)?_CLASS_ENTRY\\(.*"([^"]+)"\\s*,\\s*([^)]+)~', $file, $matches, PREG_SET_ORDER);
			foreach ($matches as $match) {
				if (preg_match('~' . preg_quote($match[2], '~') . '\\[\\](.*)\\}~sU', $file, $matches2)) {
					preg_match_all('~PHP_(?:FALIAS|ME_MAPPING|ME)\\((\\w+)\\s*,\\s*(\\w+)~', $matches2[1], $matches2, PREG_SET_ORDER);
					foreach ($matches2 as $match2) {
						$classes[$match2[1]] = $match[1];
						$method_names[strtolower($match2[2])] = strtolower("$match[1]::$match2[1]");
					}
				}
			}
		}
		
		foreach ($files as $filename => $file) {
			$file = preg_replace('~//[^\n]*|/\*.*?\*/~s', '', $file); // TODO: Respect strings. Remove #ifdef 0.
			preg_match_all('~^(?:static )?(?:ZEND|PHP)(_NAMED)?_(?:FUNCTION|METHOD)\\(([^)]+)~m', $file, $matches, PREG_SET_ORDER);
			foreach ($matches as $match) {
				$function = trim($match[1] ? $aliases[$match[2]] : $match[2]);
				if (preg_match('~^(.*\\S)\\s*,\\s*(.+)~', $function, $match)) {
					$function = (isset($classes[$match[1]]) ? $classes[$match[1]] : preg_replace_callback('~_(.)~', function ($match) {
						return strtoupper($match[1]);
					}, $match[1])) . "::$match[2]";
				}
				if (isset($method_names[$function])) {
					$function = $method_names[$function];
				}
				$return++;
				if (!isset($versions[$function][$major])) {
					$versions[$function][$major] = array($version); // Min.
				}
				$versions[$function][$major][1] = $version; // Max.
				// TODO: Gaps are ignored.
			}
		}
	}
	return $return;
}

$maxes = array(); // major => version
$nexts = array();
$prev = "";
foreach ($tags as $version) {
	if ($prev) {
		$nexts[$prev] = $version;
	}
	$prev = $version;
	$major = substr($version, 1, 1, 7);
	$maxes[$major] = $version;
}

$existing = array();
$xml = simplexml_load_file(__DIR__ . "/../.manual.xml");
foreach ($xml->getDocNamespaces() as $prefix => $namespace) {
	$xml->registerXPathNamespace(($prefix ? $prefix : "_"), $namespace);
}
foreach ($xml->xpath("//_:refname") as $refname) {
	$existing[name($refname)] = '';
}
$xml = simplexml_load_file(__DIR__ . "/../version.xml");
foreach ($xml->function as $function) {
	$existing[name($function['name'])] = $function['from']->__toString();
}

function name($name) {
	// Copied from PhD.
	return str_replace(
		array('::', '->', '__', '_', '$'),
		array('-',  '-',  '-',  '-', ''),
		strtolower($name));
}

ksort($versions);
$wrong = "";
echo "Missing versions:\n";
foreach ($versions as $function => $val) {
	$function = strtr($function, array(
		'##win32## ' => 'w32api::',
		'(oci_globals.v)' => 'OCI',
		'ceSimpleXML' => 'SimpleXML',
		'spl ## Array' => 'ArrayObject',
		'spl ## ' => '',
		'reflection::function_' => 'ReflectionFunctionAbstract',
		'tnm_ ##' => 'tidyNode::',
	));
	$print = array();
	foreach ($val as $major => $pair) {
		list($min, $max) = $pair;
		$printMin = ($min != "$major.0.0" ? " >= $min" : "");
		$printMax = ($max != $maxes[$major] ? " < $nexts[$max]" : "");
		$print[] = "PHP $major$printMin$printMax";
	}
	if (isset($existing[name($function)])) {
		if (!$existing[name($function)]) {
			echo " <function name=\"$function\" from=\"" . htmlspecialchars(implode(", ", $print)) . "\"/>\n";
		} elseif (preg_replace('~^PHP 4[^,]*, |, PECL .*~', '', $existing[name($function)]) != implode(", ", $print)) {
			$wrong .= " <function name=\"$function\" from=\"" . htmlspecialchars(implode(", ", $print)) . "\"/> <!-- " . $existing[name($function)] . " -->\n";
		}
	}
}
echo "\nWrong versions:\n$wrong";

function rglob($pattern, $dir = "") {
	foreach (glob($dir . $pattern) as $filename) {
		yield $filename;
	}
	foreach (glob("$dir*", GLOB_ONLYDIR) as $subdir) {
		foreach (rglob($pattern, "$subdir/") as $filename) {
			yield $filename;
		}
	}
}
