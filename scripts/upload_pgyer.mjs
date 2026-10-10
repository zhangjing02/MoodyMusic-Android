import fs from 'node:fs';
import path from 'node:path';

const PGYER_API_KEY = '48ceaf75791c09d36fdb364b8f1fd314';
const APK_PATH = process.argv[2] || '';
const UPDATE_DESC = process.argv[3] || '常规版本发布与体验优化';

async function main() {
  let targetApk = APK_PATH;
  if (!targetApk) {
    // 自动寻找 app/build/outputs/apk/release/ 下最新的 apk 文件
    const releaseDir = path.resolve(process.cwd(), 'app/build/outputs/apk/release');
    if (fs.existsSync(releaseDir)) {
      const apks = fs.readdirSync(releaseDir)
        .filter((f) => f.endsWith('.apk'))
        .map((f) => path.join(releaseDir, f))
        .sort((a, b) => fs.statSync(b).mtimeMs - fs.statSync(a).mtimeMs);
      if (apks.length > 0) {
        targetApk = apks[0];
      }
    }
  }

  if (!targetApk || !fs.existsSync(targetApk)) {
    console.error(`未找到有效的 APK 文件: ${targetApk}`);
    console.error('用法: node scripts/upload_pgyer.mjs [apk_path] [update_description]');
    process.exit(1);
  }

  const stat = fs.statSync(targetApk);
  console.log(`准备上传 APK: ${targetApk}`);
  console.log(`文件大小: ${(stat.size / (1024 * 1024)).toFixed(2)} MB`);
  console.log(`更新说明: ${UPDATE_DESC}`);

  console.log('\n[1/3] 正在向蒲公英请求上传凭证 (getCOSToken)...');
  const tokenParams = new URLSearchParams({
    _api_key: PGYER_API_KEY,
    buildType: 'android',
    buildUpdateDescription: UPDATE_DESC
  });

  const tokenRes = await fetch('https://www.pgyer.com/apiv2/app/getCOSToken', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: tokenParams.toString()
  });

  const tokenJson = await tokenRes.json();
  if (tokenJson.code !== 0) {
    console.error('获取上传 Token 失败:', tokenJson);
    process.exit(1);
  }

  const { endpoint, params, key } = tokenJson.data;
  console.log(`成功获取上传凭证，目标节点: ${endpoint}`);

  console.log('\n[2/3] 正在直传 APK 到蒲公英存储节点...');
  const formData = new FormData();
  for (const [k, v] of Object.entries(params)) {
    formData.append(k, v);
  }
  const fileBytes = fs.readFileSync(targetApk);
  formData.append('file', new Blob([fileBytes]), path.basename(targetApk));

  const uploadRes = await fetch(endpoint, {
    method: 'POST',
    body: formData
  });

  if (!uploadRes.ok && uploadRes.status !== 204 && uploadRes.status !== 200) {
    const errorText = await uploadRes.text();
    console.error(`上传失败 (HTTP ${uploadRes.status}):`, errorText);
    process.exit(1);
  }
  console.log('APK 文件上传成功，等待蒲公英云端解析与签名校验...');

  console.log('\n[3/3] 正在轮询蒲公英版本解析状态...');
  let buildInfo = null;
  for (let i = 0; i < 30; i++) {
    await new Promise((r) => setTimeout(r, 2000));
    try {
      const checkRes = await fetch(`https://www.pgyer.com/apiv2/app/buildInfo?_api_key=${PGYER_API_KEY}&buildKey=${key}`);
      const checkJson = await checkRes.json();
      if (checkJson.code === 0 && checkJson.data) {
        buildInfo = checkJson.data;
        break;
      } else if (checkJson.code === 1246 || checkJson.code === 1247) {
        process.stdout.write('.');
      } else {
        process.stdout.write(`[${checkJson.code}: ${checkJson.message || 'processing'}] `);
      }
    } catch (e) {
      process.stdout.write('x');
    }
  }

  console.log('\n');
  if (!buildInfo) {
    console.log(`上传已完成，可在蒲公英控制台查看，buildKey: ${key}`);
    return;
  }

  console.log('================ 蒲公英发布成功 ================');
  console.log(`应用名称: ${buildInfo.buildName}`);
  console.log(`应用包名: ${buildInfo.buildIdentifier}`);
  console.log(`版本名称: v${buildInfo.buildVersion}`);
  console.log(`构建编号: Build ${buildInfo.buildVersionNo}`);
  console.log(`更新时间: ${buildInfo.buildUpdated}`);
  console.log(`下载短链: https://www.pgyer.com/${buildInfo.buildShortcutUrl || 'yinxin-android'}`);
  console.log(`二维码图: ${buildInfo.buildQRCodeURL}`);
  console.log('=================================================');
}

main().catch((err) => {
  console.error('上传执行异常:', err);
  process.exit(1);
});
