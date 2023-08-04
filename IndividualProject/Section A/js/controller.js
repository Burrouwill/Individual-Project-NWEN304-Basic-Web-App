window.addEventListener("load",init);

let editing = false;
let currentId = 0;



function init(){
    
    clearAll();
    loadId();
    showTotal();
    bindEvents();  
}

/**
 * This function clears the contents of the form except the ID (since ID is auto generated)
 */
function clearAll(){
    document.querySelector("#id").innerText = currentId;
    document.querySelector("#name").value = "";
    document.querySelector("#desc").value = "";
    document.querySelector("#price").value = "";
    document.querySelector("#color").value = "#000000";
    document.querySelector("#url").value = "";
}

let auto = autoGen();

/**
 * this function automatically sets the value of ID
 */
function loadId(){
    document.querySelector('#id').innerText = auto.next().value;
    currentId ++;
}

/**
 * this function populates the values of #total, #mark and #unmark ids of the form
 */
function showTotal(){
    const total = itemOperations.items.length
    const mark = itemOperations.countTotalMarked();
    const unmark = total - mark;

    document.querySelector("#total").innerText = total;
    document.querySelector("#mark").innerText = mark;
    document.querySelector("#unmark").innerText = unmark;
}

function bindEvents(){
    document.querySelector('#remove').addEventListener('click',deleteRecords);
    document.querySelector('#add').addEventListener('click',addRecord);
    document.querySelector('#update').addEventListener('click',updateRecord)
    document.querySelector('#exchange').addEventListener('change',getExchangerate)
}

/**
 * this function deletes the selected record from itemOperations and prints the table using the function printTable
 */
function deleteRecords(){

    if (!editing){

        itemOperations.remove();
        printTable(itemOperations.items);
        showTotal();
    }
}

/**
 * this function adds a new record in itemOperations and then calls printRecord(). showTotal(), loadId() and clearAll()
 */
function addRecord(){
    
    if (!editing) {

    const newItem = {
        id: parseInt(document.querySelector("#id").innerText),
        name: document.querySelector("#name").value,
        description: document.querySelector('#desc').value,
        price: parseFloat(document.querySelector("#price").value),
        color: document.querySelector("#color").value,
        url: document.querySelector("#url").value,
        isMarked: false
    }
    
    

    itemOperations.add(newItem);
    printRecord(newItem);
    showTotal();
    loadId();
    clearAll();

 }
}

/**
 * this function fills (calls fillFields()) the form with the values of the item to edit after searching it in items
 */
function edit() {
    const id = this.getAttribute("data-itemid");
    const item = itemOperations.search(id)
    if (item) {
        editing = true;
        fillFields(item);
    }
}

/**
 * this function fills the form with the details of itemObject
 * @param {*} itemObject 
 */
function fillFields(itemObject) {
    document.querySelector("#id").innerText = itemObject.id
    document.querySelector("#name").value = itemObject.name
    document.querySelector("#desc").value = itemObject.desc
    document.querySelector("#price").value = itemObject.price
    document.querySelector("#color").value = itemObject.color
    document.querySelector("#url").value = itemObject.url
}


/**
 * /* this function creates icons for edit and trash for each record in the table
 * @param {*} className 
 * @param {*} fn 
 * @param {*} id 
 * @returns 
 */
function createIcon(className,fn, id){
    // <i class="fas fa-trash"></i>
    // <i class="fas fa-edit"></i>
    var iTag = document.createElement("i");
    iTag.className = className;
    iTag.addEventListener('click',fn);
    iTag.setAttribute("data-itemid", id) ;

    return iTag;
}

/**
 * this function updates the record that is edited and then prints the table using printTable()
 */
function updateRecord(){
   
    const id = parseInt(document.querySelector('#id').innerText);
    const updatedItem = {
        id: id,
        name: document.querySelector('#name').value,
        description: document.querySelector('#desc').value,
        price: parseFloat(document.querySelector('#price').value),
        color: document.querySelector("#color").value,
        url: document.querySelector("#url").value,
        isMarked: false
    };

    const itemIndex = itemOperations.items.findIndex(item => item.id === id);
    if (itemIndex !== -1) {
        itemOperations.items[itemIndex] = updatedItem;
        printTable(itemOperations.items);
        showTotal();
        
        clearAll();
    }
    editing = false;
}

/**
 * this function toggles the color of the row when its trash button is selected and updates the marked and unmarked fields
 */
function trash(){
    
    let id = this.getAttribute('data-itemid');
    itemOperations.markUnMark(id);
    showTotal();
    let tr = this.parentNode.parentNode;
    tr.classList.toggle('alert-danger');
    console.log("I am Trash ",this.getAttribute('data-itemid'))
}


/**
 * this function calls printRecord for each item of items and then calls the showTotal function
 */
function printTable(items){
   
    const tbody = document.querySelector("#items");
    tbody.innerHTML = "";

    for (const item of items){
        printRecord(item);
    }
}


function printRecord(item){
    var tbody = document.querySelector('#items');
    var tr = tbody.insertRow();
    var index = 0;
    for(let key in item){
        if(key=='isMarked'){
            continue;
        }
        let cell = tr.insertCell(index);
        cell.innerText = item[key] ;
        index++;
    }
    var lastTD = tr.insertCell(index);
    lastTD.appendChild(createIcon('fas fa-trash mr-2',trash,item.id));
    lastTD.appendChild(createIcon('fas fa-edit',edit,item.id));
}


/**
 * this function makes an AJAX call to http://apilayer.net/api/live to fetch and display the exchange rate for the currency selected
 */
function getExchangerate(){
   
    
}