window.addEventListener("load", init);

let editing = false;
let currentId = 0;



function init() {
    
    clearAll();
    loadId();
    showTotal();
    bindEvents();
}

/**
 * This function clears the contents of the form except the ID (since ID is auto generated)
 */
function clearAll() {
    document.querySelector("#id").innerText = currentId;
    document.querySelector("#name").value = "";
    document.querySelector("#desc").value = "";
    document.querySelector("#price").value = "";
    document.querySelector("#color").value = "#000000";
    document.querySelector("#url").value = "";
}

let auto = autoGen();

/**
 * this function automatically sets the value of ID. Prevents duplicate ID when data modified / loaded.
 */
function loadId() {
    if (itemOperations.items.length > 0) {
        const highestId = Math.max(...itemOperations.items.map(item => item.id));
        currentId = Math.max(currentId, highestId + 1);
    }
    const nextId = auto.next().value;
    currentId = Math.max(currentId, nextId);
    document.querySelector('#id').innerText = currentId;
}

/**
 * this function populates the values of #total, #mark and #unmark ids of the form
 */
function showTotal() {
    const total = itemOperations.items.length
    const mark = itemOperations.countTotalMarked();
    const unmark = total - mark;

    document.querySelector("#total").innerText = total;
    document.querySelector("#mark").innerText = mark;
    document.querySelector("#unmark").innerText = unmark;
}

function bindEvents() {
    document.querySelector('#remove').addEventListener('click', deleteRecords);
    document.querySelector('#add').addEventListener('click', addRecord);
    document.querySelector('#update').addEventListener('click', updateRecord);
    document.querySelector('#getExchange').addEventListener('click', getExchangerate);
    document.querySelector('#save').addEventListener('click', saveToLocalStorage);
    document.querySelector('#load').addEventListener('click', loadFromLocalStorage);
}

/**
 * this function deletes the selected record from itemOperations and prints the table using the function printTable
 */
function deleteRecords() {

    if (!editing) {

        itemOperations.remove();
        printTable(itemOperations.items);
        showTotal();
    }
}

/**
 * this function adds a new record in itemOperations and then calls printRecord(). showTotal(), loadId() and clearAll()
 */
function addRecord() {

    if (!editing) {

        const newItem = {
            id: parseInt(document.querySelector("#id").innerText),
            name: document.querySelector("#name").value,
            desc: document.querySelector('#desc').value,
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
function createIcon(className, fn, id) {
    // <i class="fas fa-trash"></i>
    // <i class="fas fa-edit"></i>
    var iTag = document.createElement("i");
    iTag.className = className;
    iTag.addEventListener('click', fn);
    iTag.setAttribute("data-itemid", id);

    return iTag;
}

/**
 * this function updates the record that is edited and then prints the table using printTable()
 */
function updateRecord() {

    const id = parseInt(document.querySelector('#id').innerText);
    const updatedItem = {
        id: id,
        name: document.querySelector('#name').value,
        desc: document.querySelector('#desc').value,
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
function trash() {

    let id = this.getAttribute('data-itemid');
    itemOperations.markUnMark(id);
    showTotal();
    let tr = this.parentNode.parentNode;
    tr.classList.toggle('alert-danger');
    console.log("I am Trash ", this.getAttribute('data-itemid'))
}


/**
 * this function calls printRecord for each item of items and then calls the showTotal function
 */
function printTable(items) {

    const tbody = document.querySelector("#items");
    tbody.innerHTML = "";

    for (const item of items) {
        printRecord(item);
    }
}


function printRecord(item) {
    var tbody = document.querySelector('#items');
    var tr = tbody.insertRow();

    const keysInOrder = ['id', 'name', 'price', 'desc', 'color', 'url'];

    for (let key of keysInOrder) {
        if (key == 'isMarked') {
            continue;
        }
        let cell = tr.insertCell();
        cell.innerText = item[key];
    }

    var lastTD = tr.insertCell();
    lastTD.appendChild(createIcon('fas fa-trash mr-2', trash, item.id));
    lastTD.appendChild(createIcon('fas fa-edit', edit, item.id));
}


/**
 * this function makes an AJAX call to http://apilayer.net/api/live to fetch and display the exchange rate for the currency selected
 */
function getExchangerate() {

    const apiUrl = "http://apilayer.net/api/live";
    const accessKey = "1b95b7e8f768d45d73e5a4d911e15c52";
    const currency = document.getElementById('exchange').value;

    const xhr = new XMLHttpRequest();
    xhr.open("GET", apiUrl + "?access_key=" + accessKey + "&currencies=" + currency, true);

    xhr.onload = function () {
        if (xhr.status >= 200 && xhr.status < 300) {
            const data = JSON.parse(xhr.responseText);
            if (data.success) {
                console.log('API response:', data);
                const quotes = data.quotes;
                const exchangeRate = quotes["USD" + currency];
                displayCurrencyConversion(exchangeRate);
            } else {
                console.error('API request was not successful:', data);
            }
        } else {
            console.error('Error fetching data:', xhr.statusText);
        }
    };
    xhr.onerror = function () {
        console.error('Request failed');
    };
    xhr.send();
}

/**
 * Saves table data to local storage
 */
function saveToLocalStorage(){
    const jsonData = JSON.stringify(itemOperations.items);
    localStorage.setItem("tableData",jsonData);
}

/**
 * Loads data from local storage to display
 */
function loadFromLocalStorage(){
    const jsonData = localStorage.getItem("tableData");

    if (jsonData){
        /* We have something to load, reset the list to prevent duplicate loads */
        itemOperations.resetItems();
        const parsedJson = JSON.parse(jsonData);
        for (const parsedItem of parsedJson){
           
            const newItem = {
                id: parsedItem.id,
                name: parsedItem.name,
                desc: parsedItem.desc,
                price: parsedItem.price,
                color: parsedItem.color,
                url: parsedItem.url,
                isMarked: parsedItem.isMarked
        }

        itemOperations.add(newItem);
    }
        clearAll();
        printTable(itemOperations.items);
        showTotal();
        loadId();
    } else {
        itemOperations.items = [];
    }
}

function loadValidId(){

}



/** Updates & displays the current exchange rate values */
function displayCurrencyConversion(exchangeRate){
    
    const currency = document.getElementById('exchange').value;
    const priceValue = parseFloat(document.querySelector("#price").value);

    if (isNaN(priceValue)) {
        document.querySelector("#exrate").value = "0.00 USD = 0.00 " + currency;
    } else {
        document.querySelector("#exrate").value = priceValue.toFixed(2) + " USD = " + (priceValue * exchangeRate).toFixed(2) + " " + currency;
    }
}