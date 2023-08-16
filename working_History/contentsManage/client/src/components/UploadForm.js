import React, {useState} from "react";
import axios from "axios";
import "./UploadForm.css";
import { toast } from 'react-toastify';
import ProgressBar from "./ProgressBar";
import { getInfo } from 'react-mediainfo'

const UploadForm = () => {
    const defaultFileName = "이미지 파일을 업로드 해주세요"
    const [file, setFile] = useState(null);
    const [imgSrc, setImgSrc] = useState(null);
    const [fileName, setFileName] = useState(defaultFileName);
    const [percent, setPercent] = useState(0);


    const imageSelectHandler = (e) => {
        const imageFile = e.target.files[0];
        setFile(imageFile);
        setFileName(imageFile.name)
        
        const fileReader = new FileReader();
        fileReader.readAsDataURL(imageFile)
        console.log(getInfo(imageFile))
        fileReader.onload = e => setImgSrc(e.target.result);
    };

    const onSubmit = async (e) => { // 기본 동작 disable - 제출 클릭시 api없으니 새로고침되지 않고 그냥 아무동작안하게끔
        e.preventDefault();
        const formData = new FormData();
        formData.append("imageTest", file) // formData로 image key 이름과 저장한 file을 지정

        try{
            const res = await axios.post("/upload",formData, {
                headers: { "Content-Type":"multipart/form-data"},
                onUploadProgress: (e) => {
                    setPercent(Math.round(100 * e.loaded/e.total))
                
                }
            });

            console.log({res});
            toast.success("업로드 성공");
            setTimeout(() => {
                setPercent(0)
                setImgSrc(null);
                setFileName(defaultFileName)
            }, 3000);
        } catch (err) {
            console.error(err);
            setPercent(0)
            setImgSrc(null);
            setFileName(defaultFileName)
            toast.error(err.message);
        }
    }

    return (
        <div>
          <form onSubmit={onSubmit}>
          <img src ={imgSrc} className={`image-preview ${imgSrc && "image-preview-show"}`}/>
          <ProgressBar percent={percent}/>
            <div className="file-dropper">
                {fileName}
                <input 
                    id="image" 
                    type="file" 
                    accept="image/*" 
                    onChange={imageSelectHandler}
                />
            </div>
            <button className="file-button" type="submit">제출</button>
          </form>
        </div>
      );
};

export default UploadForm;