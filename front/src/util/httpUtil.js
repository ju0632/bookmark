import { notification } from "antd";
import axios from "axios";

//定义http实例
const instance = axios.create({
  //   baseURL: "http://ali.tapme.top:8081/mock/16/chat/api/",
  headers: {
    token: window.token
  }
});

//实例添加拦截器
instance.interceptors.response.use(
  function(res) {
    return res.data;
  },
  function(error) {
    console.log(error);
    let message, description;
    if (error.response === undefined) {
      message = "出问题啦";
      description = "你的网络有问题";
    } else {
      message = "出问题啦:" + error.response.status;
      description = JSON.stringify(error.response.data);
      //401跳转到登录页面
    }
    notification.open({
      message,
      description,
      duration: 2
    });
    setTimeout(() => {
      if (error.response && error.response.status === 401) {
        let redirect = encodeURIComponent(window.location.pathname + window.location.search);
        window.location.replace("/public/login?redirect=" + redirect);
      }
    }, 1000);
    return Promise.reject(error);
  }
);

export default instance;
