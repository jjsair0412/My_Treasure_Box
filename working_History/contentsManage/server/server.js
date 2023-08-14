const express = require('express'); // express import 
const multer = require('multer'); // multer import ( upload 모듈 )

// const upload = multer({dest:"uploads"}) // upload 할 경로는 uploads

const {v4: uuid} = require('uuid') // uuid v4 버전 import 
const mime = require('mime-types') // mime 모듈 import . 얘는 upload 대상 파일의 type을 꺼내올 수 있음


const storage = multer.diskStorage({
    // upload 위치 . cb는 callback의 약자 . cb의 첫번째는 에러발생시 return값 , 두번째는 에러발생 안했을경우 return값
    destination: (req, file, cb) => cb(null, "./uploads"), 

    // filename , cb는 callback의 약자 cb의 첫번째는 에러발생시 return값 , 두번째는 에러발생 안했을경우 return값 . file 이름을 UUID로 변환 및 type 직접 지정하여 저장 
    filename: (req, file, cb) => cb(null, `${uuid()}.${mime.extension(file.mimetype)}`),  
});

const app = express();
const PORT = 5001;

app.use("/uploads",express.static("uploads")) // 특정 경로만 외부 노출 ( 127.0.0.1:5001:/uploads/{fileName} )

const upload = multer({storage, fileFilter: (req, file, cb)=>{
    // file type이 jpeg , png일 경우에만 allow , .includes 함수를 통해서 , 조건문에 들어간 배열이 포함되어 있다면 true 반환
    if(['image/png', 'image/jpeg', 'video/mp4'].includes(file.mimetype)) cb(null, true) 
    else cb(new Error ('invalid file type'),false)
    }, 
    limits:{
        fileSize: 1024 * 1024 * 5, // 5MB 까지만 허용
    } 
});

// route 생성
app.post("/upload", upload.single("imageTest"), (req,res) => { // multer 미들웨어 사용하여 , request의 Body 안에 들어가있는 파일을 꺼내옴 . 
    // upload.single("imageTest") 을 통해 뽑아옴 . 배열로도 가져올 수 있음 , 
    // form-data 의 imageTest 키 값을 가진 애들을 전부다 가져옴 - 필터등을 추가해서 , image 파일일 경우에만 저장하도록 제작 가능
    
    console.log(req.file)

    res.json(req.file); // file 객체를 반환

}); // post왔을때 , 두번째 람다의 request와 response 함수 (req, res) 를 실행시키라는 의미

// app.post("/upload", upload.array('videoTest'), (req,res) => {
    
//     for(i =0; i<multer.length; i++) {
//         console.log(req.file)
//         res.json(req.file); 
//     }

// }); 



app.listen(PORT,() => console.log("Express server listening on Port " + PORT)) // 설정한 port로 listen