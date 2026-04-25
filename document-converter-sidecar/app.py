import os
import hashlib
import tempfile
import re
from fastapi import FastAPI, UploadFile, HTTPException
from markitdown import MarkItDown
from typing import List, Dict, Any

app = FastAPI(title="Document Converter Sidecar")
md = MarkItDown()

def extract_headings(markdown_text: str) -> List[Dict[str, Any]]:
    sections = []
    # Match markdown headings (e.g. "## Heading", "# Heading")
    heading_pattern = re.compile(r'^(#{1,6})\s+(.*)$', re.MULTILINE)
    
    for match in heading_pattern.finditer(markdown_text):
        level = len(match.group(1))
        heading_text = match.group(2).strip()
        offset = match.start()
        
        sections.append({
            "heading": heading_text,
            "level": level,
            "charStart": offset
        })
        
    # Calculate end offsets by looking at the next heading
    for i in range(len(sections)):
        if i + 1 < len(sections):
            sections[i]["charEnd"] = sections[i+1]["charStart"]
        else:
            sections[i]["charEnd"] = len(markdown_text)
            
    # Reconstruct path based on levels
    current_path = []
    for section in sections:
        level = section["level"]
        # truncate path up to current level
        current_path = current_path[:level-1]
        # pad path with empty strings if level jumps (rare but possible)
        while len(current_path) < level - 1:
            current_path.append("")
        current_path.append(section["heading"])
        section["path"] = list(current_path)
        
    return sections

@app.post("/convert")
async def convert_document(file: UploadFile):
    if not file.filename:
        raise HTTPException(status_code=400, detail="No filename provided")
        
    # Read file content
    content = await file.read()
    
    # Calculate sha256 hash
    content_hash = hashlib.sha256(content).hexdigest()
    
    # Save to temporary file for markitdown to process
    suffix = os.path.splitext(file.filename)[1]
    if not suffix:
        suffix = ".bin"
        
    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
            tmp.write(content)
            tmp_path = tmp.name
            
        # Convert using MarkItDown
        result = md.convert(tmp_path)
        markdown_text = result.text_content
        
        # Extract sections from markdown
        sections = extract_headings(markdown_text)
        
        warnings = []
        # Basic warning detection
        if len(markdown_text.strip()) < 100:
            warnings.append("low_text_density")
            
        return {
            "documentId": file.filename,
            "markdown": markdown_text,
            "sections": sections,
            "tables": [], # Table extraction is complex, leaving for Java side or future enhancement
            "warnings": warnings,
            "converter": "markitdown",
            "contentHash": content_hash
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        if 'tmp_path' in locals() and os.path.exists(tmp_path):
            os.remove(tmp_path)

@app.get("/health")
def health_check():
    return {"status": "up"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
