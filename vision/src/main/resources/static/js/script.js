let vue = new Vue({
    el: '#app',
    data: {
        bucketName: '',
        buckets: [],
        objects: {},
        file: null
    },
    async mounted() {
        const response = await Vue.resource('buckets').get();
        const buckets = await response.json();
        this.buckets = buckets.map( b => {
            return {
                name: b.name,
                objects: []
            }
        });
        this.buckets.forEach(b => this.loadObjects(b.name));
    },
    methods: {
        async addBucket() {
            const response = await Vue.resource('buckets/add').get({name: this.bucketName});
            const bucket = await response.json();
            this.buckets.push({
                name: bucket.name,
                objects: []
            });
        },
        async deleteBucket(bucketName) {
            await Vue.resource(`buckets{/name}`).delete({name :bucketName});
            this.buckets = this.buckets.filter( bucket => {
                return bucket.name !== bucketName
            });
        },
        deleteObject(bucketName, key) {
            Vue.resource('buckets{/name}{/key}').delete({name: bucketName, key: key});
            let bucket = this.buckets.find(b => b.name == bucketName);
            bucket.objects = bucket.objects.filter(obj => obj.key != key)
        },


        async addPhoto() {
            const formData = new FormData();
            formData.append("file", event.target.files[0]);

            await axios.post('buckets/upload', formData,{
                headers:{
                    'Content-Type':'multipart/form-data'
                }
            })
                this.addPhoto();
            },


        async loadObjects(bucketName) {
            const response = await Vue.resource('buckets/object-list').get({name: bucketName});
            const objectList = await response.json();
            let bucket = this.buckets.find(b => b.name == bucketName);
            bucket.objects = objectList
        }
    }
});