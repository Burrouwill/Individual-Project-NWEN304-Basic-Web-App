const itemOperations = {
    /*Fields*/
    items:[],

    /* adds an item into the array items*/
    add(itemObject){
       this.items.push(itemObject);
    },

    /* removes the item which has the "isMarked" field set to true*/
    remove(){
         this.items = this.items.filter(item => !item.isMarked);
   },

    /* searches the item with a given argument id */
    search(id){
          return this.items.find(item => item.id == id);  
    },

    /* toggle the isMarked field of the item with the given argument id*/
    markUnMark(id){
        const item = this.search(id)
        item.isMarked = !item.isMarked
},

    /* counts the total number of marked items */
    countTotalMarked(){
       return this.items.filter(item => item.isMarked).length;
},
   
}