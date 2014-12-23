
function WorkProductsController($scope, $http) {

$scope.WorkProductNS = globals.workproductNS;
$scope.IncidentManagementNS = globals.incidentmanagementNS;



var section = 1;
$scope.section = function (id) {
section = id;
};

var IgID;


/*
 * is function
 */
$scope.is = function (id) {
return section == id;
};

$scope.workproducts = [];
$scope.currentWorkproduct = 0;


/*
 * Basic Auth function
 */
function getBasicAuth(username, password) {
var tok = username + ':' + password;
var hash = Base64.encode(tok);
return "Basic " + hash;
};

$scope.incidentMgmtEndpoint = globals.incidentMgmtPath;
$scope.workproductMgmtEndpoint = globals.workproductPath;
$scope.mapEndpoint = globals.mapPath;

$scope.init = function() {
$scope.refresh();
}



/*
 * Refrest incidents to display
 */
$scope.refresh = function() {

// render request
var xml = Mustache.render(xmlGetIncidentsTmpl, $scope);

// load agreement list
$http({
method: 'POST',
url: $scope.incidentMgmtEndpoint,
headers: {"Content-Type": "text/xml"},
data: xml }).
success(function(data, status, headers, config) {

// clear existing agreements object
$scope.workproducts = [];

// this callback will be called asynchronously
// when the response is available
var result = xmlToJSON.parseString(data);

// test to see if any agreements were returned
var workproductList =
avail(result,'Envelope[0].Body[0].GetIncidentListResponse[0].WorkProductList[0]');

// add them to local array of agreements
if (workproductList.WorkProduct && workproductList.WorkProduct.length > 0)
{
for (i = 0; i < workproductList.WorkProduct.length; i++ ) {
workproduct = workproductList.WorkProduct[i];
$scope.workproducts.push(workproduct);
}
$scope.section($scope.currentWorkproduct);
}

// Debugging:
// console.log("XML: " + data);

}).
error(function(data, status, headers, config) {
// called asynchronously if an error occurs
// or server returns response with an error status.
alert('An error occured while attempting to load the agreement list from the
core. Error code: ' + status);
});
};



/*
 * Get Details
 */

$scope.getDetails = function(workproduct) {
$scope.currentWorkProduct = $scope.workproducts.indexOf(workproduct);
var wp_data = {
identifier:
$scope.workproducts[workproduct].PackageMetadata[0].WorkProductIdentification[0].Identifier[0].text,
version:
$scope.workproducts[workproduct].PackageMetadata[0].WorkProductIdentification[0].Version[0].text,
type:
$scope.workproducts[workproduct].PackageMetadata[0].WorkProductIdentification[0].Type[0].text,
checksum:
$scope.workproducts[workproduct].PackageMetadata[0].WorkProductIdentification[0].Checksum[0].text,
state:
$scope.workproducts[workproduct].PackageMetadata[0].WorkProductIdentification[0].State[0].text,
}

// render request
var xml = Mustache.render(xmlGetProductTmpl, wp_data);
// Load Workproduct
$http({
method: 'POST',
url: $scope.workproductMgmtEndpoint,
headers: {"Content-Type": "text/xml"},
data: xml }).
success(function(data, status, headers, config) {
var result = xmlToJSON.parseString(data);

// test to see if any agreements were returned
IgID =
avail(result,'Envelope[0].Body[0].GetProductResponse[0].WorkProduct[0].PackageMetadata[0].WorkProductProperties[0].AssociatedGroups[0].Identifier[0].text');

// console.log('IgID: ' + IgID);

// viewRawXML(data);

var wp_data = {
igid: IgID
}

var xml = Mustache.render(xmlGetAllWorkProductsTmpl, wp_data);
$http({
method: 'POST',
url: $scope.workproductMgmtEndpoint,
headers: {"Content-Type": "text/xml"},
data: xml}).
success(function(data, status, headers, config) {
var result = xmlToJSON.parseString(data);
var workproductList =
avail(result,'Envelope[0].Body[0].GetAssociatedWorkProductListResponse[0].WorkProductList[0]');
var wps = "
<ERNIEDEBUG>
	";
	var mapViewContext = "";

	if (workproductList.WorkProduct && workproductList.WorkProduct.length >
	0) {
	for (i = 0; i < workproductList.WorkProduct.length; i++ ) {
	workproduct = workproductList.WorkProduct[i];
	// wps.push(workproduct);
	var dataItemID = workproduct.PackageMetadata[0].DataItemID[0].text;
	wps = wps +"
	<ENTRY>" + dataItemID + "</ENTRY>
	";
	if (dataItemID.indexOf("MapViewContext") == 0) {
	mapViewContext = dataItemID;
	}
	}
	wps = wps + "
</ERNIEDEBUG>
";

if (mapViewContext.indexOf("MapViewContext") == 0) {
/*
 * We already have a MapViewContext, so modify it
 */
// alert("Got Map: " + mapViewContext);

var wp_data = {
igid: IgID,
}

var xml = Mustache.render(xmlGetMapViewTmpl, wp_data);
$http({
method: 'POST',
url: $scope.workproductMgmtEndpoint,
headers: {"Content-Type": "text/xml"},
data: xml}).
success(function(data, status, headers, config) {
/*
 * Remove the SOAP envelope and just keep the WorkProduct data
 */
// break the textblock into an array of lines
var lines = data.split('>');
// remove 4 lines, starting at the first position
lines.splice(0,30);
// remove 3 lines at the end of the file
lines.splice(lines.length-7,7);
// join the array back into a single string
data = lines.join('>');


var url = "https://test.ernie.com";
var name = "Ernie Test";
var title = "Ernie Test title";
var format = "png";
var epsg = "EPSG:4326";
var wp_data = {
igid: IgID,
url: url,
name: name,
title: title,
format: format,
epsg: epsg
}
var layer = Mustache.render(xmlNewMapLayerTmpl, wp_data);

wp_data = {
defaultMapBody: data,
newLayer: layer,
}
var xml = Mustache.render(xmlAddMapLayerTmpl, wp_data);

$http({
method: 'POST',
url: $scope.workproductMgmtEndpoint,
headers: {"Content-Type": "text/xml"},
data: xml}).
success(function(data, status, headers, config) {
viewRawXML(data);
});
});
} else { // if MapViewContext
/*
 * Current Incident does not have a MapViewContext so we will need to create one
 */

}
// viewRawXML(wps + data);

}

});


});
};

} // End Function
